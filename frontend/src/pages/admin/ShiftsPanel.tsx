import { useCallback, useEffect, useState } from 'react'
import { endShift, getCurrentShift, isNoOpenShiftError, listShiftsByStore, startShiftOrGetCurrent } from '@/api/shifts'
import { useActiveBranch } from '@/lib/branch-store'
import { useAdminContext } from '@/pages/admin/admin-context'
import type { ShiftReport } from '@/types/models'

export function ShiftsPanel() {
  const { storeId } = useAdminContext()
  const activeBranch = useActiveBranch()
  const [shifts, setShifts] = useState<ShiftReport[]>([])
  const [current, setCurrent] = useState<ShiftReport | null>(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const [list, currentShift] = await Promise.all([
        listShiftsByStore(storeId),
        getCurrentShift().catch((err) => {
          if (isNoOpenShiftError(err)) return null
          throw err
        }),
      ])
      setShifts(list)
      setCurrent(currentShift)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load shifts')
    } finally {
      setLoading(false)
    }
  }, [storeId])

  useEffect(() => {
    void load()
  }, [load])

  async function handleStart() {
    setBusy(true)
    setError(null)
    try {
      setCurrent(
        await startShiftOrGetCurrent(activeBranch?.id != null ? { branchId: activeBranch.id } : undefined),
      )
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Start failed')
    } finally {
      setBusy(false)
    }
  }

  async function handleEnd() {
    setBusy(true)
    setError(null)
    try {
      setCurrent(await endShift())
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'End failed')
    } finally {
      setBusy(false)
    }
  }

  const open = Boolean(current && !current.shiftEnd)

  return (
    <div className="admin-panel panel">
      <div className="panel-header row-between">
        <div>
          <h2>Shifts</h2>
          <p>Shift reports for store #{storeId}</p>
        </div>
        <div className="inline-actions">
          <button type="button" className="btn btn-secondary" disabled={busy || open} onClick={() => void handleStart()}>
            Start shift
          </button>
          <button type="button" className="btn btn-secondary" disabled={busy || !open} onClick={() => void handleEnd()}>
            End shift
          </button>
          <button type="button" className="btn btn-secondary" onClick={() => void load()}>
            Refresh
          </button>
        </div>
      </div>
      <div className="panel-body">
        {error ? <div className="app-alert error">{error}</div> : null}
        <div className="app-alert">
          Current shift:{' '}
          {open
            ? `#${current?.id} started ${current?.shiftStart ? new Date(current.shiftStart).toLocaleString() : '—'}`
            : 'None open'}
        </div>
        {loading ? <p className="muted loading-msg">Loading shifts…</p> : null}
        {!loading && shifts.length === 0 ? (
          <div className="empty-state">
            <p className="muted">No shifts yet</p>
            <button type="button" className="btn btn-secondary btn-sm" onClick={() => void load()}>
              Refresh
            </button>
          </div>
        ) : null}
        {!loading && shifts.length > 0 ? (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Start</th>
                  <th>End</th>
                  <th>Branch</th>
                  <th>Cashier</th>
                  <th>Orders</th>
                  <th>Net sales</th>
                </tr>
              </thead>
              <tbody>
                {shifts.map((shift) => (
                  <tr key={shift.id}>
                    <td>{shift.id}</td>
                    <td>{shift.shiftStart ? new Date(shift.shiftStart).toLocaleString() : '—'}</td>
                    <td>{shift.shiftEnd ? new Date(shift.shiftEnd).toLocaleString() : 'Open'}</td>
                    <td>{shift.branchId ?? '—'}</td>
                    <td>{shift.cashierId ?? '—'}</td>
                    <td>{shift.totalOrders ?? 0}</td>
                    <td>${(shift.netSales ?? 0).toFixed(2)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
      </div>
    </div>
  )
}
