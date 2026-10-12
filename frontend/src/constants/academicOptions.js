/**
 * Configurable academic department and course/degree options.
 * Matches VYNK Phase 1 specifications.
 */

export const DEPARTMENT_OPTIONS = [
  'Computer Science and Engineering (CSE)',
  'Artificial Intelligence and Machine Learning (AI & ML)',
  'CSE (Artificial Intelligence)',
  'CSE (Data Science)',
  'Information Technology (IT)',
  'Electronics and Communication Engineering (ECE)',
  'Electrical and Electronics Engineering (EEE)',
  'Mechanical Engineering',
  'Civil Engineering',
  'Other',
];

export const COURSE_OPTIONS = [
  'B.Tech',
  'B.E.',
  'BCA',
  'B.Sc.',
  'M.Tech',
  'M.E.',
  'MCA',
  'M.Sc.',
  'Other',
];

/**
 * Resolves a saved or incoming department string against predefined options.
 * Preserves custom/existing data if not present in standard options.
 *
 * @param {string} rawDepartment
 * @returns {{ selectedOption: string, customDepartment: string }}
 */
export function resolveDepartment(rawDepartment) {
  if (!rawDepartment || !rawDepartment.trim()) {
    return { selectedOption: '', customDepartment: '' };
  }
  const trimmed = rawDepartment.trim();
  const found = DEPARTMENT_OPTIONS.find(
    (opt) => opt.toLowerCase() === trimmed.toLowerCase() && opt !== 'Other'
  );
  if (found) {
    return { selectedOption: found, customDepartment: '' };
  }
  // Not in predefined list -> select 'Other' and preserve existing text in custom field
  return { selectedOption: 'Other', customDepartment: trimmed };
}

/**
 * Resolves a saved or incoming course string against predefined options.
 * Preserves custom/existing data if not present in standard options.
 *
 * @param {string} rawCourse
 * @returns {{ selectedOption: string, customCourse: string }}
 */
export function resolveCourse(rawCourse) {
  if (!rawCourse || !rawCourse.trim()) {
    return { selectedOption: '', customCourse: '' };
  }
  const trimmed = rawCourse.trim();
  const found = COURSE_OPTIONS.find(
    (opt) => opt.toLowerCase() === trimmed.toLowerCase() && opt !== 'Other'
  );
  if (found) {
    return { selectedOption: found, customCourse: '' };
  }
  // Not in predefined list -> select 'Other' and preserve existing text in custom field
  return { selectedOption: 'Other', customCourse: trimmed };
}
