import { useEffect } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { fetchAnalytics } from './store/analyticsSlice'

function Bars({ data, color }) {
  const max = Math.max(1, ...Object.values(data))
  return (
    <>
      {Object.entries(data).map(([label, value]) => (
        <div className="bar-row" key={label}>
          <span className="bar-label">{label}</span>
          <div className="bar-track">
            <div className={`bar-fill ${color || label}`} style={{ width: `${(value / max) * 100}%` }} />
          </div>
          <span className="bar-value">{value}</span>
        </div>
      ))}
    </>
  )
}

export default function Analytics() {
  const dispatch = useDispatch()
  const wid = useSelector((s) => s.workspace.activeId)
  const posts = useSelector((s) => s.posts.items)
  const { data, error } = useSelector((s) => s.analytics)

  useEffect(() => {
    if (wid) dispatch(fetchAnalytics(wid))
  }, [wid, posts, dispatch])

  if (error) return <div className="alert err">{error}</div>
  if (!data) return <p className="muted">Loading analytics...</p>

  const stats = [
    ['Total posts', data.totalPosts],
    ['Impressions', data.impressions.toLocaleString()],
    ['Likes', data.likes.toLocaleString()],
    ['Shares', data.shares.toLocaleString()],
    ['Engagement rate', `${data.engagementRate}%`],
  ]

  return (
    <>
      <div className="stats">
        {stats.map(([label, value]) => (
          <div className="card stat" key={label}>
            <div className="stat-value">{value}</div>
            <div className="muted">{label}</div>
          </div>
        ))}
      </div>
      <div className="card"><h2>Posts by status</h2><Bars data={data.byStatus} /></div>
      <div className="card"><h2>Posts by platform</h2><Bars data={data.byPlatform} color="platform" /></div>
    </>
  )
}