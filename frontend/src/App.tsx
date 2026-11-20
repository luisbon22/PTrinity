import { useCallback, useState } from 'react'
import { Aviso, type AvisoDatos } from './components/Aviso'
import { ClientesPanel } from './components/ClientesPanel'
import { CuentasPanel } from './components/CuentasPanel'
import { EstadoCuentaPanel } from './components/EstadoCuentaPanel'
import { TransaccionesPanel } from './components/TransaccionesPanel'

const SECCIONES = {
  clientes: { titulo: 'Clientes', Panel: ClientesPanel },
  cuentas: { titulo: 'Productos', Panel: CuentasPanel },
  transacciones: { titulo: 'Transacciones', Panel: TransaccionesPanel },
  extracto: { titulo: 'Estado de cuenta', Panel: EstadoCuentaPanel },
} as const

type Seccion = keyof typeof SECCIONES

export default function App() {
  const [seccion, setSeccion] = useState<Seccion>('clientes')
  const [aviso, setAviso] = useState<AvisoDatos | null>(null)
  const onAviso = useCallback((a: AvisoDatos) => setAviso(a), [])
  const { Panel } = SECCIONES[seccion]

  return (
    <>
      <header className="barra sin-impresion">
        <div className="marca">
          PTrinity <span>Entidad financiera</span>
        </div>
        <nav aria-label="Secciones">
          {(Object.keys(SECCIONES) as Seccion[]).map((s) => (
            <button
              key={s}
              className={s === seccion ? 'pestana activa' : 'pestana'}
              aria-current={s === seccion ? 'page' : undefined}
              onClick={() => {
                setSeccion(s)
                setAviso(null)
              }}
            >
              {SECCIONES[s].titulo}
            </button>
          ))}
        </nav>
      </header>
      <main>
        <Aviso aviso={aviso} onCerrar={() => setAviso(null)} />
        <Panel onAviso={onAviso} />
      </main>
    </>
  )
}
