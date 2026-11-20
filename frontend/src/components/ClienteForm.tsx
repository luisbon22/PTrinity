import { useState, type FormEvent } from 'react'
import { ApiError, type Cliente, type ClienteDatos } from '../api'

interface Props {
  inicial?: Cliente
  onGuardar: (datos: ClienteDatos) => Promise<void>
  onCancelar: () => void
}

const VACIO: ClienteDatos = {
  tipoIdentificacion: 'CC',
  numeroIdentificacion: '',
  nombres: '',
  apellido: '',
  correo: '',
  fechaNacimiento: '',
}

export function ClienteForm({ inicial, onGuardar, onCancelar }: Props) {
  const [datos, setDatos] = useState<ClienteDatos>(inicial ?? VACIO)
  const [errores, setErrores] = useState<Record<string, string>>({})
  const [enviando, setEnviando] = useState(false)

  const cambiar = (campo: keyof ClienteDatos, valor: string) => setDatos({ ...datos, [campo]: valor })

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    setEnviando(true)
    setErrores({})
    try {
      await onGuardar(datos)
    } catch (error) {
      if (error instanceof ApiError) setErrores(error.errores)
    } finally {
      setEnviando(false)
    }
  }

  const campo = (nombre: keyof ClienteDatos, etiqueta: string, tipo = 'text') => (
    <label>
      {etiqueta}
      <input type={tipo} value={datos[nombre]} required onChange={(e) => cambiar(nombre, e.target.value)} />
      {errores[nombre] && <small className="error-campo">{errores[nombre]}</small>}
    </label>
  )

  return (
    <form className="tarjeta formulario" onSubmit={enviar} aria-label="Formulario de cliente">
      <h3>{inicial ? 'Editar cliente' : 'Nuevo cliente'}</h3>
      <div className="rejilla">
        <label>
          Tipo de identificación
          <select
            value={datos.tipoIdentificacion}
            onChange={(e) => cambiar('tipoIdentificacion', e.target.value)}
          >
            <option value="CC">Cédula de ciudadanía</option>
            <option value="CE">Cédula de extranjería</option>
            <option value="PASAPORTE">Pasaporte</option>
          </select>
        </label>
        {campo('numeroIdentificacion', 'Número de identificación')}
        {campo('nombres', 'Nombres')}
        {campo('apellido', 'Apellido')}
        {campo('correo', 'Correo electrónico', 'email')}
        {campo('fechaNacimiento', 'Fecha de nacimiento', 'date')}
      </div>
      <div className="acciones">
        <button type="submit" disabled={enviando}>
          Guardar
        </button>
        <button type="button" className="secundario" onClick={onCancelar}>
          Cancelar
        </button>
      </div>
    </form>
  )
}
