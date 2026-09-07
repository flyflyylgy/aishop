/**
 * 前端权限判断：登录返回的 perms 存 localStorage('admin_perms')，
 * 超管返回 ['*'] 通配。
 */
export function getPerms() {
  try {
    return JSON.parse(localStorage.getItem('admin_perms') || '[]')
  } catch {
    return []
  }
}

export function hasPerm(code) {
  const perms = getPerms()
  return perms.includes('*') || perms.includes(code)
}

export function hasAnyPerm(codes) {
  return codes.some(hasPerm)
}
