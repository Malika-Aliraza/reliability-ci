import { useState } from 'react'

const DAYS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']
const keyOf = (d) => `${d.getFullYear()}-${d.getMonth()}-${d.getDate()}`

export default function CalendarView({ posts }) {
  const [cursor, setCursor] = useState(() => {
    const d = new Date()
    return new Date(d.getFullYear(), d.getMonth(), 1)
  })
  const year = cursor.getFullYear()
  const month = cursor.getMonth()
  const offset = new Date(year, month, 1).getDay()
  const daysInMonth = new Date(year, month + 1, 0).getDate()
  const today = keyOf(new Date())

  const byDay = {}
  posts.filter((p) => p.scheduledAt).forEach((p) => {
    const k = keyOf(new Date(p.scheduledAt))
    ;(byDay[k] ||= []).push(p)
  })

  const cells = [...Array(offset).fill(null), ...Array.from({ length: daysInMonth }, (_, i) => i + 1)]

  return (
    <div className="card">
      <div className="cal-head">
        <button className="btn small" onClick={() => setCursor(new Date(year, month - 1, 1))}>&lt;</button>
        <h2>{cursor.toLocaleString([], { month: 'long', year: 'numeric' })}</h2>
        <button className="btn small" onClick={() => setCursor(new Date(year, month + 1, 1))}>&gt;</button>
      </div>
      <div className="cal-grid">
        {DAYS.map((d) => <div key={d} className="cal-dow">{d}</div>)}
        {cells.map((day, i) => {
          if (!day) return <div key={i} className="cal-cell empty" />
          const k = keyOf(new Date(year, month, day))
          return (
            <div key={i} className={k === today ? 'cal-cell today' : 'cal-cell'}>
              <div className="cal-day">{day}</div>
              {(byDay[k] || []).map((p) => (
                <div key={p.id} className={`cal-post ${p.status}`} title={p.content}>
                  {new Date(p.scheduledAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} {p.platform}
                </div>
              ))}
            </div>
          )
        })}
      </div>
    </div>
  )
}