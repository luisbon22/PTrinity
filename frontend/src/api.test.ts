import { afterEach, describe, expect, it, vi } from 'vitest'
import { api, ApiError, mensajeDeError } from './api'

function responder(status: number, cuerpo?: unknown) {
  const fetchMock = vi.fn().mockResolvedValue({
    ok: status >= 200 && status < 300,
    status,
    json: () => (cuerpo === undefined ? Promise.reject(new Error('sin cuerpo')) : Promise.resolve(cuerpo)),
  })
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

afterEach(() => vi.unstubAllGlobals())

describe('api', () => {
  it('envía JSON y devuelve la respuesta', async () => {
    const fetchMock = responder(201, { id: 1 })
    const datos = {
      tipoIdentificacion: 'CC' as const,
      numeroIdentificacion: '1',
      nombres: 'Ana',
      apellido: 'Gomez',
      correo: 'a@b.co',
      fechaNacimiento: '1990-01-01',
    }

    await expect(api.crearCliente(datos)).resolves.toEqual({ id: 1 })
    expect(fetchMock).toHaveBeenCalledWith('/api/clientes', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(datos),
    })
  })

  it('arma las URLs de cada operación', async () => {
    const fetchMock = responder(200, {})
    await api.listarClientes()
    await api.actualizarCliente(2, {} as never)
    await api.listarCuentas()
    await api.listarCuentas(3)
    await api.abrirCuenta(3, 'AHORROS', true)
    await api.cambiarExencion(4, false)
    await api.cambiarEstado(4, 'INACTIVA')
    await api.estadoDeCuenta(4, '2026-01-01', '2026-01-31')
    await api.listarMovimientos(4)
    await api.consignar(4, 10, '')
    await api.retirar(4, 10, '')
    await api.transferir(4, 5, 10, '')
    const urls = fetchMock.mock.calls.map((c) => `${c[1].method} ${c[0]}`)
    expect(urls).toEqual([
      'GET /api/clientes',
      'PUT /api/clientes/2',
      'GET /api/cuentas',
      'GET /api/cuentas?clienteId=3',
      'POST /api/cuentas',
      'PUT /api/cuentas/4',
      'PATCH /api/cuentas/4/estado',
      'GET /api/cuentas/4/estado-de-cuenta?desde=2026-01-01&hasta=2026-01-31',
      'GET /api/transacciones?cuentaId=4',
      'POST /api/transacciones/consignaciones',
      'POST /api/transacciones/retiros',
      'POST /api/transacciones/transferencias',
    ])
  })

  it('204 devuelve undefined', async () => {
    responder(204)
    await expect(api.eliminarCliente(1)).resolves.toBeUndefined()
  })

  it('convierte los Problem Details en ApiError', async () => {
    responder(400, { detail: 'Revise los campos', errores: { correo: 'inválido' } })
    const error = await api.listarClientes().catch((e) => e)
    expect(error).toBeInstanceOf(ApiError)
    expect(error.estado).toBe(400)
    expect(error.errores).toEqual({ correo: 'inválido' })
  })

  it('error sin cuerpo usa un mensaje genérico', async () => {
    responder(500)
    await expect(api.listarClientes()).rejects.toThrow('Error inesperado del servidor')
  })
})

describe('mensajeDeError', () => {
  it('incluye los errores por campo', () => {
    expect(mensajeDeError(new ApiError(400, 'Datos inválidos', { correo: 'malo' }))).toBe('Datos inválidos. correo: malo')
  })

  it('usa el detalle cuando no hay campos', () => {
    expect(mensajeDeError(new ApiError(422, 'Saldo insuficiente'))).toBe('Saldo insuficiente')
  })

  it('error de red', () => {
    expect(mensajeDeError(new TypeError('fetch failed'))).toBe('No fue posible conectar con el servidor')
  })
})
