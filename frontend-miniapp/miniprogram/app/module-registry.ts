import { accountModule } from '../features/account/module'
import { merchantModule } from '../features/merchant/module'
import { messageModule } from '../features/message/module'
import { productModule } from '../features/product/module'
import { tradeModule } from '../features/trade/module'

export const moduleRegistry = [
  accountModule,
  merchantModule,
  productModule,
  tradeModule,
  messageModule,
] as const

