export interface AvisoDatos {
  tipo: 'exito' | 'error'
  texto: string
}

interface Props {
  aviso: AvisoDatos | null
  onCerrar: () => void
}

export function Aviso({ aviso, onCerrar }: Props) {
  if (!aviso) return null
  return (
    <div className={`aviso aviso-${aviso.tipo}`} role={aviso.tipo === 'error' ? 'alert' : 'status'}>
      <span>{aviso.texto}</span>
      <button type="button" className="boton-texto" onClick={onCerrar} aria-label="Cerrar aviso">
        ×
      </button>
    </div>
  )
}
