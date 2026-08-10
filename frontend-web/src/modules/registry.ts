import { accountModule } from './account'
import { merchantModule } from './merchant'
import { messageModule } from './message'
import { productModule } from './product'
import { tradeModule } from './trade'

export const webModules = [accountModule, merchantModule, productModule, tradeModule, messageModule]

export const webModuleRoutes = webModules.flatMap((module) => module.routes)
export const webModuleMenuItems = webModules.flatMap((module) => module.menuItems)
