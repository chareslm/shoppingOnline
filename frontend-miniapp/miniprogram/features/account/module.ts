import type { AppModuleContribution } from '../../app/module-types'

export const accountModule: AppModuleContribution = {
  key: 'account',
  owner: '项目管理员',
  pages: [
    '/pages/home/index',
    '/pages/profile/index',
    '/pages/addresses/index',
    '/pages/preferences/index',
    '/pages/change-password/index',
  ],
}

