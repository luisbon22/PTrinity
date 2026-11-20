import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { api, mensajeDeError, type Cuenta, type Movimiento } from '../api'
import type { AvisoDatos } from './Aviso'
import { TablaMovimientos } from './TablaMovimientos'

type Operacion = 'consignar' | 'retirar' | 'transferir'

interface Props {
  onAviso: (aviso: AvisoDatos) => void
}

export function TransaccionesPanel({ onAviso }: Props) {
  const [cuentas, setCuentas] = useState<Cuenta[]>([])
  const [operacion, setOperacion] = useState<Operacion>('consignar')
  const [cuentaId, setCuentaId] = useState('')
  const [destinoId, setDestinoId] = useState('')
  const [monto, setMonto] = useState('')
  const [descripcion, setDescripcion] = useState('')
  const [movimientos, setMovimientos] = useState<Movimiento[]>([])

  const error = useCallback((e: unknown) => onAviso({ tipo: 'error', texto: mensajeDeError(e) }), [onAviso])

  const cargarMovimientos = useCallback(
    (id: string) => {
      if (!id) return setMovimientos([])
      api.listarMovimientos(Number(id)).then(setMovimientos).catch(error)
    },
    [error],
  )

  const cargarCuentas = useCallback(() => {
    api.listarCuentas().then((todas) => setCuentas(todas.filter((c) => c.estado === 'ACTIVA'))).catch(error)
  }, [error])

  useEffect(cargarCuentas, [cargarCuentas])
  useEffect(() => cargarMovimientos(cuentaId), [cuentaId, cargarMovimientos])

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    const id = Number(cuentaId)
    const valor = Number(monto)
    try {
      if (operacion === 'consignar') await api.consignar(id, valor, descripcion)
      else if (operacion === 'retirar') await api.retirar(id, valor, descripcion)
      else await api.transferir(id, Number(destinoId), valor, descripcion)
      onAviso({ tipo: 'exito', texto: 'Transacción realizada' })
      setMonto('')
      setDescripcion('')
      cargarCuentas()
      cargarMovimientos(cuentaId)
    } catch (e) {
      error(e)
    }
  }

  const opciones = (excluir?: string) =>
    cuentas
      .filter((c) => String(c.id) !== excluir)
      .map((c) => (
        <option key={c.id} value={c.id}>
          {c.numeroCuenta} · {c.tipoCuenta === 'AHORROS' ? 'Ahorros' : 'Corriente'}
        </option>
      ))

  return (
    <section>
      <h2>Transacciones</h2>
      <form className="tarjeta formulario" onSubmit={enviar} aria-label="Nueva transacción">
        <div className="pestanas-internas" role="radiogroup" aria-label="Tipo de transacción">
          {(['consignar', 'retirar', 'transferir'] as const).map((op) => (
            <label key={op} className={operacion === op ? 'activa' : ''}>
              <input
                type="radio"
                name="operacion"
                value={op}
                checked={operacion === op}
                onChange={() => setOperacion(op)}
              />
              {op === 'consignar' ? 'Consignación' : op === 'retirar' ? 'Retiro' : 'Transferencia'}
            </label>
          ))}
        </div>
        <div className="rejilla">
          <label>
            {operacion === 'transferir' ? 'Cuenta origen' : 'Cuenta'}
            <select value={cuentaId} required onChange={(e) => setCuentaId(e.target.value)}>
              <option value="">Seleccione…</option>
              {opciones()}
            </select>
          </label>
          {operacion === 'transferir' && (
            <label>
              Cuenta destino
              <select value={destinoId} required onChange={(e) => setDestinoId(e.target.value)}>
                <option value="">Seleccione…</option>
                {opciones(cuentaId)}
              </select>
            </label>
          )}
          <label>
            Monto
            <input type="number" min="0.01" step="0.01" value={monto} required onChange={(e) => setMonto(e.target.value)} />
          </label>
          <label>
            Descripción
            <input value={descripcion} maxLength={255} onChange={(e) => setDescripcion(e.target.value)} />
          </label>
        </div>
        <div className="acciones">
          <button type="submit">Realizar</button>
        </div>
      </form>
      {cuentaId && (
        <>
          <h3>Movimientos de la cuenta</h3>
          <TablaMovimientos movimientos={movimientos} />
        </>
      )}
    </section>
  )
}
