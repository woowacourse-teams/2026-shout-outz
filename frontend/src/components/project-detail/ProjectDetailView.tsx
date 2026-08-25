import type { Project } from '../../api/project';

export function ProjectDetailView({ project }: { project: Project }) {
    return (
        <section>
            <h1 className="text-2xl font-bold">{project.name}</h1>
            <p className="mt-2 text-gray-600">{project.tagline}</p>
            <p className="mt-4">{project.description}</p>
            <div className="mt-4 flex gap-2">
                {project.techTags.map((tag) => (
                    <span key={tag} className="rounded bg-gray-100 px-2 py-1 text-sm">
                        {tag}
                    </span>
                ))}
            </div>
        </section>
    );
}
