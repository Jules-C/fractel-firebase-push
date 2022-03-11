import { WebPlugin } from '@capacitor/core';

import type { FirebasePushPluginPlugin } from './definitions';

export class FirebasePushPluginWeb
  extends WebPlugin
  implements FirebasePushPluginPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
