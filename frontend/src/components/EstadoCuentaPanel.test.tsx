import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { api, ApiError } from '../api'
import { cuenta, extracto } from '../pruebas/datos'
import { EstadoCuentaPanel } from './EstadoCuentaPanel'

vi.mock('../api', async (original) => {
  const real = await original<typeof import('../api')>()
  return { ...real, api: { listarCuentas: vi.fn(), estadoDeCuenta: vi.fn() } }
})

const mockApi = vi.mocked(api)

describe('EstadoCuentaPanel', () => {
  const onAviso = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
    mockApi.listarCuentas.mockResolvedValue([cuenta()])
  })

  async function consultar(user: ReturnType<typeof userEvent.setup>) {
    await screen.findByText('5310000001 · ACTIVA')
    await user.selectOptions(screen.getByLabelText('Cuenta'), '1')
    await user.clear(screen.getByLabelText('Desde'))
    await user.type(screen.getByLabelText('Desde'), '2026-01-01')
    await user.clear(screen.getByLabelText('Hasta'))
    await user.type(screen.getByLabelText('Hasta'), '2026-01-31')
    await user.click(screen.getByRole('button', { name: 'Consultar' }))
  }

  it('muestra el extracto con totales, contraparte y GMF', async () => {
    mockApi.estadoDeCuenta.mockResolvedValue(extracto)
    const imprimir = vi.spyOn(window, 'print').mockImplementation(() => {})
    const user = userEvent.setup()
    render(<EstadoCuentaPanel onAviso={onAviso} />)
    await consultar(user)

    expect(await screen.findByText('Ana Gomez')).toBeInTheDocument()
    expect(mockApi.estadoDeCuenta).toHaveBeenCalledWith(1, '2026-01-01', '2026-01-31')
    expect(screen.getByText('Pago arriendo')).toBeInTheDocument()
    expect(screen.getByText(/3310000001/)).toBeInTheDocument()
    expect(screen.getByText('Transferencia')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Imprimir' }))
    expect(imprimir).toHaveBeenCalled()
  })

  it('muestra cuenta corriente sin movimientos', async () => {
    mockApi.estadoDeCuenta.mockResolvedValue({
      ...extracto,
      cuenta: cuenta({ tipoCuenta: 'CORRIENTE', numeroCuenta: '3310000001' }),
      movimientos: [],
    })
    const user = userEvent.setup()
    render(<EstadoCuentaPanel onAviso={onAviso} />)
    await consultar(user)

    expect(await screen.findByText(/Cuenta corriente/)).toBeInTheDocument()
    expect(screen.getByText('Sin movimientos')).toBeInTheDocument()
  })

  it('reporta errores de consulta y de carga', async () => {
    mockApi.estadoDeCuenta.mockRejectedValue(new ApiError(422, 'La fecha inicial no puede ser posterior a la final'))
    const user = userEvent.setup()
    render(<EstadoCuentaPanel onAviso={onAviso} />)
    await consultar(user)
    await waitFor(() =>
      expect(onAviso).toHaveBeenCalledWith({ tipo: 'error', texto: 'La fecha inicial no puede ser posterior a la final' }),
    )

    mockApi.listarCuentas.mockRejectedValue(new ApiError(500, 'Caído'))
    render(<EstadoCuentaPanel onAviso={onAviso} />)
    await waitFor(() => expect(onAviso).toHaveBeenCalledWith({ tipo: 'error', texto: 'Caído' }))
  })
})
