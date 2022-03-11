export interface FirebasePushPluginPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
