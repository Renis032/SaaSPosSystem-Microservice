import { ApiError, apiClient } from '@/lib/api-client'
import type { ShiftReport } from '@/types/models'

export type StartShiftBody = {
  branchId?: number
}

export function startShift(body?: StartShiftBody) {
  return apiClient<ShiftReport>('/api/shift-report/start', {
    method: 'POST',
    body: body?.branchId != null ? { branchId: body.branchId } : undefined,
  })
}

export function isNoOpenShiftError(err: unknown) {
  return (
    err instanceof ApiError &&
    (err.status === 404 || /no open shift/i.test(err.message))
  )
}

/** Start a shift, or return the existing open one if this cashier already has one. */
export async function startShiftOrGetCurrent(body?: StartShiftBody) {
  try {
    return await startShift(body)
  } catch (err) {
    if (err instanceof ApiError && /already has an open shift/i.test(err.message)) {
      return getCurrentShift()
    }
    throw err
  }
}

export function endShift() {
  return apiClient<ShiftReport>('/api/shift-report/end', { method: 'PATCH' })
}

export function getCurrentShift() {
  return apiClient<ShiftReport>('/api/shift-report/current')
}

export function listShiftsByStore(storeId: number) {
  return apiClient<ShiftReport[]>(`/api/shift-report/store/${storeId}`)
}
