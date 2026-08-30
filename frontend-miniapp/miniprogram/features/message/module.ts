import type { AppModuleContribution } from '../../app/module-types'

export const messageModule: AppModuleContribution = {
  key: 'message',
  owner: '成员 5',
  pages: [
    '/package-message/pages/chat/index',
    '/package-message/pages/thread/index',
    '/package-message/pages/notifications/index',
  ],
}
