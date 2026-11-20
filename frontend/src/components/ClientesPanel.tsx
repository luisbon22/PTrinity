import { useCallback, useEffect, useState } from 'react'
import { api, mensajeDeError, type Cliente, type ClienteDatos } from '../api'
import { formatoFecha } from '../formato'
import type { AvisoDatos } from './Aviso'
import { ClienteForm } from './ClienteForm'

interface Props {
  onAviso: (aviso: AvisoDatos) => void
}

export function ClientesPanel({ onAviso }: Props) {
  const [clientes, setClientes] = useState<Cliente[]>([])
  // undefined: formulario cerrado; null: nuevo cliente; Cliente: edicion
  const [editando, setEditando] = useState<Cliente | null | undefined>(undefined)

  const cargar = useCallback(() => {
    api.listarClientes().then(setClientes).catch((e) => onAviso({ tipo: 'error', texto: mensajeDeError(e) }))
  }, [onAviso])

  useEffect(cargar, [cargar])

  async function guardar(datos: ClienteDatos) {
    try {
      if (editando) await api.actualizarCliente(editando.id, datos)
      else await api.crearCliente(datos)
      onAviso({ tipo: 'exito', texto: editando ? 'Cliente actualizado' : 'Cliente registrado' })
      setEditando(undefined)
      cargar()
    } catch (error) {
      onAviso({ tipo: 'error', texto: mensajeDeError(error) })
      throw error
    }
  }

  async function eliminar(cliente: Cliente) {
    if (!window.confirm(`¿Eliminar a ${cliente.nombres} ${cliente.apellido}?`)) return
    try {
      await api.eliminarCliente(cliente.id)
      onAviso({ tipo: 'exito', texto: 'Cliente eliminado' })
      cargar()
    } catch (error) {
      onAviso({ tipo: 'error', texto: mensajeDeError(error) })
    }
  }

  return (
    <section>
      <div className="encabezado-seccion">
        <h2>Clientes</h2>
        {editando === undefined && <button onClick={() => setEditando(null)}>Nuevo cliente</button>}
      </div>
      {editando !== undefined && (
        <ClienteForm
          key={editando?.id ?? 'nuevo'}
          inicial={editando ?? undefined}
          onGuardar={guardar}
          onCancelar={() => setEditando(undefined)}
        />
      )}
      <div className="tabla-contenedor">
        <table>
          <thead>
            <tr>
              <th>Identificación</th>
              <th>Nombre</th>
              <th>Correo</th>
              <th>Nacimiento</th>
              <th>Creado</th>
              <th>Modificado</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {clientes.length === 0 && (
              <tr>
                <td colSpan={7} className="vacio">
                  No hay clientes registrados
                </td>
              </tr>
            )}
            {clientes.map((c) => (
              <tr key={c.id}>
                <td>
                  {c.tipoIdentificacion} {c.numeroIdentificacion}
                </td>
                <td>
                  {c.nombres} {c.apellido}
                </td>
                <td>{c.correo}</td>
                <td>{c.fechaNacimiento}</td>
                <td>{formatoFecha(c.fechaCreacion)}</td>
                <td>{formatoFecha(c.fechaModificacion)}</td>
                <td className="acciones">
                  <button className="secundario" onClick={() => setEditando(c)}>
                    Editar
                  </button>
                  <button className="peligro" onClick={() => eliminar(c)}>
                    Eliminar
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  )
}
