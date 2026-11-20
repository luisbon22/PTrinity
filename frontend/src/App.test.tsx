import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { api, ApiError } from './api'
import App from './App'

vi.mock('./api', async (original) => {
  const real = await original<typeof import('./api')>()
  return { ...real, api: { listarClientes: vi.fn(), listarCuentas: vi.fn(), listarMovimientos: vi.fn() } }
})

const mockApi = vi.mocked(api)

describe('App', () => {
  beforeEach(() => {
    mockApi.listarClientes.mockResolvedValue([])
    mockApi.listarCuentas.mockResolvedValue([])
  })

  it('navega entre las secciones', async () => {
    const user = userEvent.setup()
    render(<App />)
    expect(screen.getByRole('heading', { name: 'Clientes' })).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Productos' }))
    expect(screen.getByRole('heading', { name: 'Productos financieros' })).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Transacciones' }))
    expect(screen.getByRole('heading', { name: 'Transacciones' })).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Estado de cuenta' }))
    expect(screen.getByRole('heading', { name: 'Estado de cuenta' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Estado de cuenta' })).toHaveAttribute('aria-current', 'page')
  })

  it('muestra y cierra los avisos', async () => {
    mockApi.listarClientes.mockRejectedValue(new ApiError(500, 'Servidor caído'))
    const user = userEvent.setup()
    render(<App />)

    expect(await screen.findByRole('alert')).toHaveTextContent('Servidor caído')
    await user.click(screen.getByRole('button', { name: 'Cerrar aviso' }))
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })
})
