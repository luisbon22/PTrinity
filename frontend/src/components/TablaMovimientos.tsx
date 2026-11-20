import type { Movimiento } from '../api'
import { formatoFecha, formatoPesos } from '../formato'

const NOMBRE_TIPO = { CONSIGNACION: 'Consignación', RETIRO: 'Retiro', TRANSFERENCIA: 'Transferencia' } as const

export function TablaMovimientos({ movimientos }: { movimientos: Movimiento[] }) {
  return (
    <div className="tabla-contenedor">
      <table>
        <thead>
          <tr>
            <th>Fecha</th>
            <th>Tipo</th>
            <th>Detalle</th>
            <th className="numero">Crédito</th>
            <th className="numero">Débito</th>
            <th className="numero">GMF</th>
            <th className="numero">Saldo</th>
          </tr>
        </thead>
        <tbody>
          {movimientos.length === 0 && (
            <tr>
              <td colSpan={7} className="vacio">
                Sin movimientos
              </td>
            </tr>
          )}
          {movimientos.map((m) => (
            <tr key={m.id}>
              <td>{formatoFecha(m.fecha)}</td>
              <td>{NOMBRE_TIPO[m.tipo]}</td>
              <td>
                {m.descripcion}
                {m.cuentaContraparte && (
                  <small className="contraparte">
                    {m.naturaleza === 'DEBITO' ? ' → ' : ' ← '}
                    {m.cuentaContraparte}
                  </small>
                )}
              </td>
              <td className="numero credito">{m.naturaleza === 'CREDITO' ? formatoPesos(m.monto) : ''}</td>
              <td className="numero debito">{m.naturaleza === 'DEBITO' ? formatoPesos(m.monto) : ''}</td>
              <td className="numero">{m.gmf > 0 ? formatoPesos(m.gmf) : ''}</td>
              <td className="numero">{formatoPesos(m.saldoResultante)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
