export class ProjectNotApprovedError extends Error {
  constructor() {
    super('승인되지 않은 프로젝트입니다.');
    this.name = 'ProjectNotApprovedError';
  }
}
