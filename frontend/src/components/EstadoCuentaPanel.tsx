import { useEffect, useState, type FormEvent } from 'react'
import { api, mensajeDeError, type Cuenta, type EstadoDeCuenta } from '../api'
import { formatoPesos, hoy, primerDiaDelMes } from '../formato'
import type { AvisoDatos } from './Aviso'
import { TablaMovimientos } from './TablaMovimientos'

interface Props {
  onAviso: (aviso: AvisoDatos) => void
}

export function EstadoCuentaPanel({ onAviso }: Props) {
  const [cuentas, setCuentas] = useState<Cuenta[]>([])
  const [cuentaId, setCuentaId] = useState('')
  const [desde, setDesde] = useState(primerDiaDelMes())
  const [hasta, setHasta] = useState(hoy())
  const [extracto, setExtracto] = useState<EstadoDeCuenta | null>(null)

  useEffect(() => {
    api.listarCuentas().then(setCuentas).catch((e) => onAviso({ tipo: 'error', texto: mensajeDeError(e) }))
  }, [onAviso])

  async function consultar(evento: FormEvent) {
    evento.preventDefault()
    try {
      setExtracto(await api.estadoDeCuenta(Number(cuentaId), desde, hasta))
    } catch (e) {
      onAviso({ tipo: 'error', texto: mensajeDeError(e) })
    }
  }

  return (
    <section>
      <h2>Estado de cuenta</h2>
      <form className="tarjeta formulario sin-impresion" onSubmit={consultar} aria-label="Consultar estado de cuenta">
        <div className="rejilla">
          <label>
            Cuenta
            <select value={cuentaId} required onChange={(e) => setCuentaId(e.target.value)}>
              <option value="">Seleccione…</option>
              {cuentas.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.numeroCuenta} · {c.estado}
                </option>
              ))}
            </select>
          </label>
          <label>
            Desde
            <input type="date" value={desde} required onChange={(e) => setDesde(e.target.value)} />
          </label>
          <label>
            Hasta
            <input type="date" value={hasta} required onChange={(e) => setHasta(e.target.value)} />
          </label>
        </div>
        <div className="acciones">
          <button type="submit">Consultar</button>
          {extracto && (
            <button type="button" className="secundario" onClick={() => window.print()}>
              Imprimir
            </button>
          )}
        </div>
      </form>

      {extracto && (
        <article className="tarjeta extracto">
          <header>
            <div>
              <strong>{extracto.titular}</strong>
              <div>{extracto.identificacionTitular}</div>
            </div>
            <div>
              Cuenta {extracto.cuenta.tipoCuenta === 'AHORROS' ? 'de ahorros' : 'corriente'} N.º{' '}
              {extracto.cuenta.numeroCuenta}
              <div>
                Periodo {extracto.desde} a {extracto.hasta}
              </div>
            </div>
          </header>
          <dl className="resumen">
            <div>
              <dt>Créditos</dt>
              <dd className="credito">{formatoPesos(extracto.totalCreditos)}</dd>
            </div>
            <div>
              <dt>Débitos (incluye GMF)</dt>
              <dd className="debito">{formatoPesos(extracto.totalDebitos)}</dd>
            </div>
            <div>
              <dt>Saldo actual</dt>
              <dd>{formatoPesos(extracto.cuenta.saldo)}</dd>
            </div>
            <div>
              <dt>Saldo disponible</dt>
              <dd>{formatoPesos(extracto.cuenta.saldoDisponible)}</dd>
            </div>
          </dl>
          <TablaMovimientos movimientos={extracto.movimientos} />
        </article>
      )}
    </section>
  )
}
