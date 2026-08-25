import { queryOptions } from '@tanstack/react-query';
import mockProjects from './mock/projects.json';

export interface Project {
    id: string;
    name: string;
    tagline: string;
    description: string;
    techTags: string[];
}

export interface Maker {
    name: string;
    bio: string;
}

interface ProjectRecord extends Project {
    maker: Maker;
}

const PROJECTS = mockProjects as ProjectRecord[];

function findProjectRecord(id: string): ProjectRecord | undefined {
    return PROJECTS.find((project) => project.id === id);
}

// 실제 API가 연결되기 전까지 SSG 파이프라인 검증용으로 지연을 흉내내는 mock입니다.
export function fetchProjects(): Promise<Project[]> {
    return new Promise((resolve) => {
        setTimeout(() => resolve(PROJECTS.map(({ maker: _maker, ...project }) => project)), 50);
    });
}

export function fetchProject(id: string): Promise<Project> {
    return new Promise((resolve, reject) => {
        setTimeout(() => {
            const record = findProjectRecord(id);
            if (!record) {
                reject(new Error(`프로젝트를 찾을 수 없습니다: ${id}`));
                return;
            }
            const { maker: _maker, ...project } = record;
            resolve(project);
        }, 50);
    });
}

export function fetchMaker(projectId: string): Promise<Maker> {
    return new Promise((resolve, reject) => {
        setTimeout(() => {
            const record = findProjectRecord(projectId);
            if (!record) {
                reject(new Error(`제작자를 찾을 수 없습니다: ${projectId}`));
                return;
            }
            resolve(record.maker);
        }, 50);
    });
}

export function projectsQueryOptions() {
    return queryOptions({
        queryKey: ['projects'],
        queryFn: fetchProjects,
    });
}

export function projectQueryOptions(id: string) {
    return queryOptions({
        queryKey: ['project', id],
        queryFn: () => fetchProject(id),
    });
}

export function makerQueryOptions(projectId: string) {
    return queryOptions({
        queryKey: ['maker', projectId],
        queryFn: () => fetchMaker(projectId),
    });
}
