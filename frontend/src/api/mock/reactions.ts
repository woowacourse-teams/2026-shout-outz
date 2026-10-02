interface MockProjectReaction {
  likeCount: number;
  likedByMe: boolean;
}

const projectReactions = new Map<string, MockProjectReaction>();

export function getMockProjectReaction(slug: string, initialLikeCount: number) {
  const current = projectReactions.get(slug) ?? {
    likeCount: initialLikeCount,
    likedByMe: false,
  };
  projectReactions.set(slug, current);
  return current;
}

export function setMockProjectLike(slug: string, initialLikeCount: number, active: boolean) {
  const current = getMockProjectReaction(slug, initialLikeCount);
  if (current.likedByMe !== active) {
    current.likedByMe = active;
    current.likeCount = Math.max(0, current.likeCount + (active ? 1 : -1));
  }
  return current;
}
