import { describe, expect, it } from 'vitest'
import { formatoFecha, formatoPesos, hoy, primerDiaDelMes } from './formato'

describe('formato', () => {
  it('pesos colombianos con dos decimales', () => {
    expect(formatoPesos(1500000)).toMatch(/1\.500\.000,00/)
  })

  it('fecha ISO legible', () => {
    expect(formatoFecha('2026-01-15T10:30:45')).toBe('2026-01-15 10:30')
  })

  it('hoy y primer día del mes en formato ISO', () => {
    expect(hoy()).toMatch(/^\d{4}-\d{2}-\d{2}$/)
    expect(primerDiaDelMes()).toBe(hoy().slice(0, 8) + '01')
  })
})
