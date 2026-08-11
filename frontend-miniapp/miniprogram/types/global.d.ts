import type { AppModuleContribution } from '../app/module-types'

declare global {
  interface IAppOption {
    globalData: {
      modules: readonly AppModuleContribution[]
    }
  }
}

export {}
