const pesos = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', minimumFractionDigits: 2 })

export const formatoPesos = (valor: number) => pesos.format(valor)

/** "2026-01-15T10:00:00" -> "2026-01-15 10:00" */
export const formatoFecha = (iso: string) => iso.replace('T', ' ').slice(0, 16)

export const hoy = () => new Date().toLocaleDateString('en-CA')

export const primerDiaDelMes = () => hoy().slice(0, 8) + '01'
