import styles from "./templates.module.css";

interface Props {
  categories: string[];
  selected: string | null;
  onSelect: (category: string | null) => void;
}

export function CategoryFilter({ categories, selected, onSelect }: Props) {
  const options: { value: string | null; label: string }[] = [
    { value: null, label: "全部" },
    ...categories.map((c) => ({ value: c, label: c })),
  ];
  return (
    <div className={styles.filter} role="group" aria-label="模板分类">
      {options.map((option) => {
        const active = option.value === selected;
        return (
          <button
            key={option.label}
            type="button"
            className={`${styles.chip} ${active ? styles.chipActive : ""}`}
            aria-pressed={active}
            onClick={() => onSelect(option.value)}
          >
            {option.label}
          </button>
        );
      })}
    </div>
  );
}
