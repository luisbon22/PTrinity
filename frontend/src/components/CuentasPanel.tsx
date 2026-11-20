import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { api, mensajeDeError, type Cliente, type Cuenta, type EstadoCuenta, type TipoCuenta } from '../api'
import { formatoFecha, formatoPesos } from '../formato'
import type { AvisoDatos } from './Aviso'

interface Props {
  onAviso: (aviso: AvisoDatos) => void
}

export function CuentasPanel({ onAviso }: Props) {
  const [clientes, setClientes] = useState<Cliente[]>([])
  const [cuentas, setCuentas] = useState<Cuenta[]>([])
  const [filtro, setFiltro] = useState('')
  const [clienteId, setClienteId] = useState('')
  const [tipo, setTipo] = useState<TipoCuenta>('AHORROS')
  const [exenta, setExenta] = useState(false)

  const error = useCallback((e: unknown) => onAviso({ tipo: 'error', texto: mensajeDeError(e) }), [onAviso])

  const cargar = useCallback(() => {
    api.listarCuentas(filtro ? Number(filtro) : undefined).then(setCuentas).catch(error)
  }, [filtro, error])

  useEffect(() => {
    api.listarClientes().then(setClientes).catch(error)
  }, [error])
  useEffect(cargar, [cargar])

  const nombre = (id: number) => {
    const c = clientes.find((x) => x.id === id)
    return c ? `${c.nombres} ${c.apellido}` : `#${id}`
  }

  async function ejecutar(accion: Promise<Cuenta>, exito: string) {
    try {
      await accion
      onAviso({ tipo: 'exito', texto: exito })
      cargar()
    } catch (e) {
      error(e)
    }
  }

  function abrir(evento: FormEvent) {
    evento.preventDefault()
    ejecutar(api.abrirCuenta(Number(clienteId), tipo, exenta), 'Cuenta creada')
  }

  const cambiarEstado = (cuenta: Cuenta, estado: EstadoCuenta) => {
    if (estado === 'CANCELADA' && !window.confirm(`¿Cancelar la cuenta ${cuenta.numeroCuenta}?`)) return
    ejecutar(api.cambiarEstado(cuenta.id, estado), `Cuenta ${cuenta.numeroCuenta}: ${estado}`)
  }

  return (
    <section>
      <h2>Productos financieros</h2>
      <form className="tarjeta formulario" onSubmit={abrir} aria-label="Abrir cuenta">
        <h3>Abrir cuenta</h3>
        <div className="rejilla">
          <label>
            Cliente
            <select value={clienteId} required onChange={(e) => setClienteId(e.target.value)}>
              <option value="">Seleccione…</option>
              {clientes.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.nombres} {c.apellido} ({c.numeroIdentificacion})
                </option>
              ))}
            </select>
          </label>
          <label>
            Tipo de cuenta
            <select value={tipo} onChange={(e) => setTipo(e.target.value as TipoCuenta)}>
              <option value="AHORROS">Ahorros</option>
              <option value="CORRIENTE">Corriente</option>
            </select>
          </label>
          <label className="casilla">
            <input type="checkbox" checked={exenta} onChange={(e) => setExenta(e.target.checked)} />
            Exenta de GMF
          </label>
        </div>
        <div className="acciones">
          <button type="submit">Abrir cuenta</button>
        </div>
      </form>

      <label className="filtro">
        Filtrar por cliente
        <select value={filtro} onChange={(e) => setFiltro(e.target.value)}>
          <option value="">Todos</option>
          {clientes.map((c) => (
            <option key={c.id} value={c.id}>
              {c.nombres} {c.apellido}
            </option>
          ))}
        </select>
      </label>

      <div className="tabla-contenedor">
        <table>
          <thead>
            <tr>
              <th>Número</th>
              <th>Tipo</th>
              <th>Titular</th>
              <th>Estado</th>
              <th className="numero">Saldo</th>
              <th className="numero">Disponible</th>
              <th>GMF</th>
              <th>Creada</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {cuentas.length === 0 && (
              <tr>
                <td colSpan={9} className="vacio">
                  No hay cuentas
                </td>
              </tr>
            )}
            {cuentas.map((c) => (
              <tr key={c.id}>
                <td>{c.numeroCuenta}</td>
                <td>{c.tipoCuenta === 'AHORROS' ? 'Ahorros' : 'Corriente'}</td>
                <td>{nombre(c.clienteId)}</td>
                <td>
                  <span className={`etiqueta etiqueta-${c.estado.toLowerCase()}`}>{c.estado}</span>
                </td>
                <td className="numero">{formatoPesos(c.saldo)}</td>
                <td className="numero">{formatoPesos(c.saldoDisponible)}</td>
                <td>
                  <label className="casilla">
                    <input
                      type="checkbox"
                      aria-label={`Exenta GMF ${c.numeroCuenta}`}
                      checked={c.exentaGmf}
                      disabled={c.estado === 'CANCELADA'}
                      onChange={(e) => ejecutar(api.cambiarExencion(c.id, e.target.checked), 'Exención de GMF actualizada')}
                    />
                    Exenta
                  </label>
                </td>
                <td>{formatoFecha(c.fechaCreacion)}</td>
                <td className="acciones">
                  {c.estado === 'ACTIVA' && (
                    <button className="secundario" onClick={() => cambiarEstado(c, 'INACTIVA')}>
                      Inactivar
                    </button>
                  )}
                  {c.estado === 'INACTIVA' && (
                    <button className="secundario" onClick={() => cambiarEstado(c, 'ACTIVA')}>
                      Activar
                    </button>
                  )}
                  {c.estado !== 'CANCELADA' && (
                    <button className="peligro" onClick={() => cambiarEstado(c, 'CANCELADA')}>
                      Cancelar
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  )
}
