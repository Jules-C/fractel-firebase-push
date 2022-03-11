import { registerPlugin } from '@capacitor/core';

import type { FirebasePushPluginPlugin } from './definitions';

const FirebasePushPlugin = registerPlugin<FirebasePushPluginPlugin>(
  'FirebasePushPlugin',
  {
    web: () => import('./web').then(m => new m.FirebasePushPluginWeb()),
  },
);

export * from './definitions';
export { FirebasePushPlugin };
