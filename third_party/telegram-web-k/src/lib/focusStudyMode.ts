export type FocusStudyPeerType = 'channel' | 'supergroup' | 'group' | 'user' | 'unknown';

export interface FocusGuardStudyBridge {
  onLoginSuccess(): void;
  isPeerApproved(peerId: string, peerType: string): boolean;
  onBlockedPeer(peerId: string, peerType: string, title: string, username: string, sourceUrl: string): void;
}

declare global {
  interface Window {
    FocusGuardStudyBridge?: FocusGuardStudyBridge;
  }
}

export function isFocusGuardStudyMode() {
  return !!window.FocusGuardStudyBridge;
}

export function isFocusStudyPeerAllowed(peerId: PeerId, peer: any) {
  const bridge = window.FocusGuardStudyBridge;
  if(!bridge || !peerId || peerId === 0) return true;

  let peerType: FocusStudyPeerType = 'unknown';
  if(peer?._ === 'user') {
    peerType = 'user';
  } else if(peer?._ === 'chat') {
    peerType = 'group';
  } else if(peer?._ === 'channel') {
    peerType = peer.pFlags?.megagroup ? 'supergroup' : 'channel';
  }

  if(peerType !== 'unknown' && bridge.isPeerApproved(String(peerId), peerType)) {
    return true;
  }

  const title = peer?.title || [peer?.first_name, peer?.last_name].filter(Boolean).join(' ') || 'Telegram destination';
  const usernames = peer?.usernames;
  const username = peer?.username || (Array.isArray(usernames) ? usernames.find((item) => item?.active)?.username : '') || '';
  bridge.onBlockedPeer(String(peerId), peerType, title, username, window.location.href);
  return false;
}
