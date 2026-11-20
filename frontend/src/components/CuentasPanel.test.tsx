import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { api, ApiError } from '../api'
import { cliente, cuenta } from '../pruebas/datos'
import { CuentasPanel } from './CuentasPanel'

vi.mock('../api', async (original) => {
  const real = await original<typeof import('../api')>()
  return {
    ...real,
    api: {
      listarClientes: vi.fn(),
      listarCuentas: vi.fn(),
      abrirCuenta: vi.fn(),
      cambiarEstado: vi.fn(),
      cambiarExencion: vi.fn(),
    },
  }
})

const mockApi = vi.mocked(api)

describe('CuentasPanel', () => {
  const onAviso = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
    mockApi.listarClientes.mockResolvedValue([cliente])
    mockApi.listarCuentas.mockResolvedValue([
      cuenta(),
      cuenta({ id: 2, tipoCuenta: 'CORRIENTE', numeroCuenta: '3310000001', estado: 'INACTIVA', clienteId: 99 }),
      cuenta({ id: 3, numeroCuenta: '5310000003', estado: 'CANCELADA', saldo: 0 }),
    ])
    mockApi.cambiarEstado.mockResolvedValue(cuenta())
  })

  it('lista las cuentas con titular, estado y saldos', async () => {
    render(<CuentasPanel onAviso={onAviso} />)
    const fila = (await screen.findByText('5310000001')).closest('tr')!
    expect(within(fila).getByText('Ana Gomez')).toBeInTheDocument()
    expect(within(fila).getByText('ACTIVA')).toBeInTheDocument()
    expect(screen.getByText('#99')).toBeInTheDocument()
    expect(screen.getByRole('cell', { name: 'Corriente' })).toBeInTheDocument()
  })

  it('muestra mensaje cuando no hay cuentas y filtra por cliente', async () => {
    mockApi.listarCuentas.mockResolvedValue([])
    const user = userEvent.setup()
    render(<CuentasPanel onAviso={onAviso} />)
    expect(await screen.findByText('No hay cuentas')).toBeInTheDocument()

    await user.selectOptions(screen.getByLabelText('Filtrar por cliente'), '1')
    await waitFor(() => expect(mockApi.listarCuentas).toHaveBeenLastCalledWith(1))
  })

  it('abre una cuenta corriente exenta', async () => {
    mockApi.abrirCuenta.mockResolvedValue(cuenta())
    const user = userEvent.setup()
    render(<CuentasPanel onAviso={onAviso} />)
    const form = screen.getByRole('form', { name: 'Abrir cuenta' })

    await screen.findAllByText('Ana Gomez (1020304050)')
    await user.selectOptions(within(form).getByLabelText('Cliente'), '1')
    await user.selectOptions(within(form).getByLabelText('Tipo de cuenta'), 'CORRIENTE')
    await user.click(within(form).getByLabelText('Exenta de GMF'))
    await user.click(within(form).getByRole('button', { name: 'Abrir cuenta' }))

    await waitFor(() => expect(mockApi.abrirCuenta).toHaveBeenCalledWith(1, 'CORRIENTE', true))
    expect(onAviso).toHaveBeenCalledWith({ tipo: 'exito', texto: 'Cuenta creada' })
  })

  it('inactiva, activa y cancela con confirmación', async () => {
    const confirmar = vi.spyOn(window, 'confirm')
    const user = userEvent.setup()
    render(<CuentasPanel onAviso={onAviso} />)
    await screen.findByText('5310000001')

    await user.click(screen.getByRole('button', { name: 'Inactivar' }))
    await user.click(screen.getByRole('button', { name: 'Activar' }))
    await waitFor(() => expect(mockApi.cambiarEstado).toHaveBeenCalledWith(1, 'INACTIVA'))
    expect(mockApi.cambiarEstado).toHaveBeenCalledWith(2, 'ACTIVA')

    const [cancelarPrimera] = screen.getAllByRole('button', { name: 'Cancelar' })
    confirmar.mockReturnValueOnce(false)
    await user.click(cancelarPrimera)
    expect(mockApi.cambiarEstado).not.toHaveBeenCalledWith(1, 'CANCELADA')

    confirmar.mockReturnValueOnce(true)
    mockApi.cambiarEstado.mockRejectedValueOnce(new ApiError(422, 'Solo con saldo $0'))
    await user.click(cancelarPrimera)
    await waitFor(() => expect(onAviso).toHaveBeenCalledWith({ tipo: 'error', texto: 'Solo con saldo $0' }))
  })

  it('cambia la exención de GMF', async () => {
    mockApi.cambiarExencion.mockResolvedValue(cuenta({ exentaGmf: true }))
    const user = userEvent.setup()
    render(<CuentasPanel onAviso={onAviso} />)

    await user.click(await screen.findByLabelText('Exenta GMF 5310000001'))

    await waitFor(() => expect(mockApi.cambiarExencion).toHaveBeenCalledWith(1, true))
    expect(screen.getByLabelText('Exenta GMF 5310000003')).toBeDisabled()
  })
})
