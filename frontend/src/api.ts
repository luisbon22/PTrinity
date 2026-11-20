// Cliente HTTP de la API REST de PTrinity. Unico punto que conoce URLs y formatos de error.

export type TipoIdentificacion = 'CC' | 'CE' | 'PASAPORTE'
export type TipoCuenta = 'AHORROS' | 'CORRIENTE'
export type EstadoCuenta = 'ACTIVA' | 'INACTIVA' | 'CANCELADA'

export interface ClienteDatos {
  tipoIdentificacion: TipoIdentificacion
  numeroIdentificacion: string
  nombres: string
  apellido: string
  correo: string
  fechaNacimiento: string
}

export interface Cliente extends ClienteDatos {
  id: number
  fechaCreacion: string
  fechaModificacion: string
}

export interface Cuenta {
  id: number
  tipoCuenta: TipoCuenta
  numeroCuenta: string
  estado: EstadoCuenta
  saldo: number
  saldoDisponible: number
  exentaGmf: boolean
  clienteId: number
  fechaCreacion: string
  fechaModificacion: string
}

export interface Movimiento {
  id: number
  cuentaId: number
  tipo: 'CONSIGNACION' | 'RETIRO' | 'TRANSFERENCIA'
  naturaleza: 'CREDITO' | 'DEBITO'
  monto: number
  gmf: number
  saldoResultante: number
  referencia: string
  cuentaContraparte: string | null
  descripcion: string | null
  fecha: string
}

export interface EstadoDeCuenta {
  cuenta: Cuenta
  titular: string
  identificacionTitular: string
  desde: string
  hasta: string
  totalCreditos: number
  totalDebitos: number
  movimientos: Movimiento[]
}

export class ApiError extends Error {
  readonly estado: number
  readonly errores: Record<string, string>

  constructor(estado: number, mensaje: string, errores: Record<string, string> = {}) {
    super(mensaje)
    this.estado = estado
    this.errores = errores
  }
}

async function solicitar<T>(url: string, metodo = 'GET', cuerpo?: unknown): Promise<T> {
  const respuesta = await fetch(url, {
    method: metodo,
    headers: cuerpo === undefined ? undefined : { 'Content-Type': 'application/json' },
    body: cuerpo === undefined ? undefined : JSON.stringify(cuerpo),
  })
  if (respuesta.status === 204) return undefined as T
  const datos = await respuesta.json().catch(() => ({}))
  if (!respuesta.ok) {
    throw new ApiError(respuesta.status, datos.detail ?? 'Error inesperado del servidor', datos.errores)
  }
  return datos as T
}

export const api = {
  listarClientes: () => solicitar<Cliente[]>('/api/clientes'),
  crearCliente: (datos: ClienteDatos) => solicitar<Cliente>('/api/clientes', 'POST', datos),
  actualizarCliente: (id: number, datos: ClienteDatos) => solicitar<Cliente>(`/api/clientes/${id}`, 'PUT', datos),
  eliminarCliente: (id: number) => solicitar<void>(`/api/clientes/${id}`, 'DELETE'),

  listarCuentas: (clienteId?: number) =>
    solicitar<Cuenta[]>(clienteId ? `/api/cuentas?clienteId=${clienteId}` : '/api/cuentas'),
  abrirCuenta: (clienteId: number, tipoCuenta: TipoCuenta, exentaGmf: boolean) =>
    solicitar<Cuenta>('/api/cuentas', 'POST', { clienteId, tipoCuenta, exentaGmf }),
  cambiarExencion: (id: number, exentaGmf: boolean) => solicitar<Cuenta>(`/api/cuentas/${id}`, 'PUT', { exentaGmf }),
  cambiarEstado: (id: number, estado: EstadoCuenta) =>
    solicitar<Cuenta>(`/api/cuentas/${id}/estado`, 'PATCH', { estado }),
  estadoDeCuenta: (id: number, desde: string, hasta: string) =>
    solicitar<EstadoDeCuenta>(`/api/cuentas/${id}/estado-de-cuenta?desde=${desde}&hasta=${hasta}`),

  listarMovimientos: (cuentaId: number) => solicitar<Movimiento[]>(`/api/transacciones?cuentaId=${cuentaId}`),
  consignar: (cuentaId: number, monto: number, descripcion: string) =>
    solicitar<Movimiento>('/api/transacciones/consignaciones', 'POST', { cuentaId, monto, descripcion }),
  retirar: (cuentaId: number, monto: number, descripcion: string) =>
    solicitar<Movimiento>('/api/transacciones/retiros', 'POST', { cuentaId, monto, descripcion }),
  transferir: (cuentaOrigenId: number, cuentaDestinoId: number, monto: number, descripcion: string) =>
    solicitar<{ debito: Movimiento; credito: Movimiento }>('/api/transacciones/transferencias', 'POST', {
      cuentaOrigenId,
      cuentaDestinoId,
      monto,
      descripcion,
    }),
}

export function mensajeDeError(error: unknown): string {
  if (error instanceof ApiError) {
    const campos = Object.entries(error.errores).map(([campo, msg]) => `${campo}: ${msg}`)
    return campos.length ? `${error.message}. ${campos.join('; ')}` : error.message
  }
  return 'No fue posible conectar con el servidor'
}
