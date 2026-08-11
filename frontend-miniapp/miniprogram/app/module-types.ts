export interface AppModuleContribution {
  key: 'account' | 'merchant' | 'product' | 'trade' | 'message'
  owner: string
  pages: readonly string[]
}

