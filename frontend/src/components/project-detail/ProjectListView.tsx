import { Link } from '@tanstack/react-router';
import type { Project } from '../../api/project';

export function ProjectListView({ projects }: { projects: Project[] }) {
    return (
        <div className="mx-auto max-w-3xl px-4 py-10">
            <h1 className="text-2xl font-bold">프로젝트 목록</h1>
            <ul className="mt-6 divide-y">
                {projects.map((project) => (
                    <li key={project.id} className="py-4">
                        <Link to="/project-detail/$id" params={{ id: project.id }} className="block">
                            <p className="font-medium">{project.name}</p>
                            <p className="text-gray-600">{project.tagline}</p>
                        </Link>
                    </li>
                ))}
            </ul>
        </div>
    );
}
