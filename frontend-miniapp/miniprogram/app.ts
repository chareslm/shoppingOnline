import { moduleRegistry } from './app/module-registry'

App<IAppOption>({
  globalData: {
    modules: moduleRegistry,
  },
})
