interface MockProjectReaction {
  likeCount: number;
  likedByMe: boolean;
}

const projectReactions = new Map<number, MockProjectReaction>();

export function getMockProjectReaction(projectId: number, initialLikeCount: number) {
  const current = projectReactions.get(projectId) ?? {
    likeCount: initialLikeCount,
    likedByMe: false,
  };
  projectReactions.set(projectId, current);
  return current;
}

export function setMockProjectLike(projectId: number, initialLikeCount: number, active: boolean) {
  const current = getMockProjectReaction(projectId, initialLikeCount);
  if (current.likedByMe !== active) {
    current.likedByMe = active;
    current.likeCount = Math.max(0, current.likeCount + (active ? 1 : -1));
  }
  return current;
}
