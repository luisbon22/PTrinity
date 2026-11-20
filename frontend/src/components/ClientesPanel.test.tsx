import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { api, ApiError } from '../api'
import { cliente } from '../pruebas/datos'
import { ClientesPanel } from './ClientesPanel'

vi.mock('../api', async (original) => {
  const real = await original<typeof import('../api')>()
  return {
    ...real,
    api: { listarClientes: vi.fn(), crearCliente: vi.fn(), actualizarCliente: vi.fn(), eliminarCliente: vi.fn() },
  }
})

const mockApi = vi.mocked(api)

describe('ClientesPanel', () => {
  const onAviso = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
    mockApi.listarClientes.mockResolvedValue([cliente])
  })

  it('lista los clientes con sus fechas', async () => {
    render(<ClientesPanel onAviso={onAviso} />)
    expect(await screen.findByText('Ana Gomez')).toBeInTheDocument()
    expect(screen.getByText('2026-01-06 09:00')).toBeInTheDocument()
  })

  it('muestra mensaje cuando no hay clientes', async () => {
    mockApi.listarClientes.mockResolvedValue([])
    render(<ClientesPanel onAviso={onAviso} />)
    expect(await screen.findByText('No hay clientes registrados')).toBeInTheDocument()
  })

  it('avisa si no puede cargar', async () => {
    mockApi.listarClientes.mockRejectedValue(new ApiError(500, 'Caído'))
    render(<ClientesPanel onAviso={onAviso} />)
    await waitFor(() => expect(onAviso).toHaveBeenCalledWith({ tipo: 'error', texto: 'Caído' }))
  })

  it('registra un cliente nuevo', async () => {
    mockApi.crearCliente.mockResolvedValue(cliente)
    const user = userEvent.setup()
    render(<ClientesPanel onAviso={onAviso} />)

    await user.click(await screen.findByRole('button', { name: 'Nuevo cliente' }))
    await user.selectOptions(screen.getByLabelText('Tipo de identificación'), 'CE')
    await user.type(screen.getByLabelText('Número de identificación'), '999')
    await user.type(screen.getByLabelText('Nombres'), 'Luis')
    await user.type(screen.getByLabelText('Apellido'), 'Diaz')
    await user.type(screen.getByLabelText('Correo electrónico'), 'luis@correo.com')
    await user.type(screen.getByLabelText('Fecha de nacimiento'), '1980-02-02')
    await user.click(screen.getByRole('button', { name: 'Guardar' }))

    await waitFor(() =>
      expect(mockApi.crearCliente).toHaveBeenCalledWith({
        tipoIdentificacion: 'CE',
        numeroIdentificacion: '999',
        nombres: 'Luis',
        apellido: 'Diaz',
        correo: 'luis@correo.com',
        fechaNacimiento: '1980-02-02',
      }),
    )
    expect(onAviso).toHaveBeenCalledWith({ tipo: 'exito', texto: 'Cliente registrado' })
    expect(screen.queryByRole('form', { name: 'Formulario de cliente' })).not.toBeInTheDocument()
  })

  it('muestra los errores por campo que devuelve la API', async () => {
    mockApi.crearCliente.mockRejectedValue(new ApiError(400, 'Datos inválidos', { correo: 'formato inválido' }))
    const user = userEvent.setup()
    render(<ClientesPanel onAviso={onAviso} />)

    await user.click(await screen.findByRole('button', { name: 'Nuevo cliente' }))
    await user.type(screen.getByLabelText('Número de identificación'), '1')
    await user.type(screen.getByLabelText('Nombres'), 'Lu')
    await user.type(screen.getByLabelText('Apellido'), 'Di')
    await user.type(screen.getByLabelText('Correo electrónico'), 'x@y.co')
    await user.type(screen.getByLabelText('Fecha de nacimiento'), '1980-02-02')
    await user.click(screen.getByRole('button', { name: 'Guardar' }))

    expect(await screen.findByText('formato inválido')).toBeInTheDocument()
    expect(onAviso).toHaveBeenCalledWith({ tipo: 'error', texto: 'Datos inválidos. correo: formato inválido' })
  })

  it('edita un cliente y permite cancelar', async () => {
    mockApi.actualizarCliente.mockResolvedValue(cliente)
    const user = userEvent.setup()
    render(<ClientesPanel onAviso={onAviso} />)

    await user.click(await screen.findByRole('button', { name: 'Editar' }))
    expect(screen.getByLabelText('Nombres')).toHaveValue('Ana')
    await user.click(screen.getByRole('button', { name: 'Cancelar' }))
    expect(screen.queryByLabelText('Nombres')).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Editar' }))
    await user.clear(screen.getByLabelText('Apellido'))
    await user.type(screen.getByLabelText('Apellido'), 'Gómez')
    await user.click(screen.getByRole('button', { name: 'Guardar' }))

    await waitFor(() =>
      expect(mockApi.actualizarCliente).toHaveBeenCalledWith(1, expect.objectContaining({ apellido: 'Gómez' })),
    )
    expect(onAviso).toHaveBeenCalledWith({ tipo: 'exito', texto: 'Cliente actualizado' })
  })

  it('elimina solo tras confirmar y reporta el rechazo del negocio', async () => {
    const confirmar = vi.spyOn(window, 'confirm')
    const user = userEvent.setup()
    render(<ClientesPanel onAviso={onAviso} />)
    const boton = await screen.findByRole('button', { name: 'Eliminar' })

    confirmar.mockReturnValueOnce(false)
    await user.click(boton)
    expect(mockApi.eliminarCliente).not.toHaveBeenCalled()

    confirmar.mockReturnValueOnce(true)
    mockApi.eliminarCliente.mockResolvedValueOnce(undefined)
    await user.click(boton)
    await waitFor(() => expect(onAviso).toHaveBeenCalledWith({ tipo: 'exito', texto: 'Cliente eliminado' }))

    confirmar.mockReturnValueOnce(true)
    mockApi.eliminarCliente.mockRejectedValueOnce(new ApiError(422, 'Tiene productos vinculados'))
    await user.click(boton)
    await waitFor(() => expect(onAviso).toHaveBeenCalledWith({ tipo: 'error', texto: 'Tiene productos vinculados' }))
  })
})
