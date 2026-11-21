import {afterEach, describe, expect, test, vi} from 'vitest';
import {isFocusStudyPeerAllowed} from '@lib/focusStudyMode';

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('Focus Guard study peer gate', () => {
  test('allows a peer only when both its exact Web K ID and type are approved', () => {
    vi.stubGlobal('FocusGuardStudyBridge', {
      isPeerApproved: vi.fn((peerId: string, peerType: string) => peerId === '-4812' && peerType === 'supergroup'),
      onBlockedPeer: vi.fn()
    });

    expect(isFocusStudyPeerAllowed(-4812 as PeerId, {_:'channel', pFlags:{megagroup:true}, title:'Study group'})).toBe(true);
    expect(isFocusStudyPeerAllowed(-4812 as PeerId, {_:'channel', pFlags:{megagroup:false}, title:'Study channel'})).toBe(false);
    expect(window.FocusGuardStudyBridge?.onBlockedPeer).toHaveBeenCalledWith(
      '-4812', 'channel', 'Study channel', '', window.location.href
    );
  });

  test('rejects personal users and reports verified peer details for native approval', () => {
    const onBlockedPeer = vi.fn();
    vi.stubGlobal('FocusGuardStudyBridge', {
      isPeerApproved: vi.fn(() => false),
      onBlockedPeer
    });

    expect(isFocusStudyPeerAllowed(9123 as PeerId, {
      _: 'user',
      first_name: 'Personal',
      last_name: 'Contact',
      username: 'contact'
    })).toBe(false);
    expect(onBlockedPeer).toHaveBeenCalledWith(
      '9123', 'user', 'Personal Contact', 'contact', window.location.href
    );
  });
});
