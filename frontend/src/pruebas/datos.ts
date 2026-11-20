import type { Cliente, Cuenta, EstadoDeCuenta, Movimiento } from '../api'

export const cliente: Cliente = {
  id: 1,
  tipoIdentificacion: 'CC',
  numeroIdentificacion: '1020304050',
  nombres: 'Ana',
  apellido: 'Gomez',
  correo: 'ana@correo.com',
  fechaNacimiento: '1990-05-14',
  fechaCreacion: '2026-01-05T09:00:00',
  fechaModificacion: '2026-01-06T09:00:00',
}

export function cuenta(cambios: Partial<Cuenta> = {}): Cuenta {
  return {
    id: 1,
    tipoCuenta: 'AHORROS',
    numeroCuenta: '5310000001',
    estado: 'ACTIVA',
    saldo: 1500000,
    saldoDisponible: 1500000,
    exentaGmf: false,
    clienteId: 1,
    fechaCreacion: '2026-01-05T09:10:00',
    fechaModificacion: '2026-01-05T09:10:00',
    ...cambios,
  }
}

export function movimiento(cambios: Partial<Movimiento> = {}): Movimiento {
  return {
    id: 1,
    cuentaId: 1,
    tipo: 'CONSIGNACION',
    naturaleza: 'CREDITO',
    monto: 2000000,
    gmf: 0,
    saldoResultante: 2000000,
    referencia: 'ref',
    cuentaContraparte: null,
    descripcion: 'Consignación inicial',
    fecha: '2026-01-05T09:15:00',
    ...cambios,
  }
}

export const extracto: EstadoDeCuenta = {
  cuenta: cuenta(),
  titular: 'Ana Gomez',
  identificacionTitular: 'CC 1020304050',
  desde: '2026-01-01',
  hasta: '2026-01-31',
  totalCreditos: 2000000,
  totalDebitos: 500000,
  movimientos: [
    movimiento(),
    movimiento({
      id: 2,
      tipo: 'TRANSFERENCIA',
      naturaleza: 'DEBITO',
      monto: 500000,
      gmf: 2000,
      cuentaContraparte: '3310000001',
      descripcion: 'Pago arriendo',
    }),
  ],
}
