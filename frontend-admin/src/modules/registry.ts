import { merchantModule } from './merchant'
import { messageModule } from './message'
import { productModule } from './product'
import { systemModule } from './system'
import { tradeModule } from './trade'

export const adminModules = [systemModule, merchantModule, productModule, tradeModule, messageModule]

export const adminModuleRoutes = adminModules.flatMap((module) => module.routes)
export const adminModuleMenuItems = adminModules.flatMap((module) => module.menuItems)
