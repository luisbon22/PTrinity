import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { api, ApiError } from '../api'
import { cuenta, movimiento } from '../pruebas/datos'
import { TransaccionesPanel } from './TransaccionesPanel'

vi.mock('../api', async (original) => {
  const real = await original<typeof import('../api')>()
  return {
    ...real,
    api: { listarCuentas: vi.fn(), listarMovimientos: vi.fn(), consignar: vi.fn(), retirar: vi.fn(), transferir: vi.fn() },
  }
})

const mockApi = vi.mocked(api)

describe('TransaccionesPanel', () => {
  const onAviso = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
    mockApi.listarCuentas.mockResolvedValue([
      cuenta(),
      cuenta({ id: 2, tipoCuenta: 'CORRIENTE', numeroCuenta: '3310000001' }),
      cuenta({ id: 3, numeroCuenta: '5310000003', estado: 'INACTIVA' }),
    ])
    mockApi.listarMovimientos.mockResolvedValue([movimiento()])
    mockApi.consignar.mockResolvedValue(movimiento())
    mockApi.retirar.mockResolvedValue(movimiento())
  })

  async function elegirCuenta(user: ReturnType<typeof userEvent.setup>, etiqueta = 'Cuenta') {
    await screen.findAllByText('5310000001 · Ahorros')
    await user.selectOptions(screen.getByLabelText(etiqueta), '1')
  }

  it('solo ofrece cuentas activas y muestra sus movimientos', async () => {
    const user = userEvent.setup()
    render(<TransaccionesPanel onAviso={onAviso} />)
    await elegirCuenta(user)

    expect(screen.queryByText(/5310000003/)).not.toBeInTheDocument()
    expect(await screen.findByText('Consignación inicial')).toBeInTheDocument()
    expect(mockApi.listarMovimientos).toHaveBeenCalledWith(1)
  })

  it('consigna y limpia el formulario', async () => {
    const user = userEvent.setup()
    render(<TransaccionesPanel onAviso={onAviso} />)
    await elegirCuenta(user)
    await user.type(screen.getByLabelText('Monto'), '1000')
    await user.type(screen.getByLabelText('Descripción'), 'Ventanilla')
    await user.click(screen.getByRole('button', { name: 'Realizar' }))

    await waitFor(() => expect(mockApi.consignar).toHaveBeenCalledWith(1, 1000, 'Ventanilla'))
    expect(onAviso).toHaveBeenCalledWith({ tipo: 'exito', texto: 'Transacción realizada' })
    expect(screen.getByLabelText('Monto')).toHaveValue(null)
  })

  it('retira y reporta saldo insuficiente', async () => {
    mockApi.retirar.mockRejectedValue(new ApiError(422, 'Saldo insuficiente'))
    const user = userEvent.setup()
    render(<TransaccionesPanel onAviso={onAviso} />)
    await user.click(screen.getByLabelText('Retiro'))
    await elegirCuenta(user)
    await user.type(screen.getByLabelText('Monto'), '99999999')
    await user.click(screen.getByRole('button', { name: 'Realizar' }))

    await waitFor(() => expect(onAviso).toHaveBeenCalledWith({ tipo: 'error', texto: 'Saldo insuficiente' }))
  })

  it('transfiere sin permitir la misma cuenta como destino', async () => {
    mockApi.transferir.mockResolvedValue({ debito: movimiento(), credito: movimiento() })
    const user = userEvent.setup()
    render(<TransaccionesPanel onAviso={onAviso} />)
    await user.click(screen.getByLabelText('Transferencia'))
    await elegirCuenta(user, 'Cuenta origen')

    const destino = screen.getByLabelText('Cuenta destino')
    expect(within(destino).queryByText('5310000001 · Ahorros')).not.toBeInTheDocument()
    await user.selectOptions(destino, '2')
    await user.type(screen.getByLabelText('Monto'), '500')
    await user.click(screen.getByRole('button', { name: 'Realizar' }))

    await waitFor(() => expect(mockApi.transferir).toHaveBeenCalledWith(1, 2, 500, ''))
  })

  it('avisa si no carga las cuentas', async () => {
    mockApi.listarCuentas.mockRejectedValue(new TypeError('red'))
    render(<TransaccionesPanel onAviso={onAviso} />)
    await waitFor(() =>
      expect(onAviso).toHaveBeenCalledWith({ tipo: 'error', texto: 'No fue posible conectar con el servidor' }),
    )
  })
})
