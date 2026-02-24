declare module 'sockjs-client' {
  interface SockJSConstructor {
    new (url: string, options?: object): WebSocket;
    CONNECTING: number;
    OPEN: number;
    CLOSING: number;
    CLOSED: number;
  }
  const SockJS: SockJSConstructor;
  export default SockJS;
}
