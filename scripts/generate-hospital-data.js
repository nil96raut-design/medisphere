const fs = require('fs');
const crypto = require('crypto');
const uuid = () => crypto.randomUUID();

const doctors = [
  { doctorId: 'DOC-001', name: 'Dr. Arvind Mehta', specialization: 'Cardiology' },
  { doctorId: 'DOC-002', name: 'Dr. Sunita Reddy', specialization: 'General Medicine' },
  { doctorId: 'DOC-003', name: 'Dr. Prakash Nair', specialization: 'Orthopedics' },
  { doctorId: 'DOC-004', name: 'Dr. Anjali Deshmukh', specialization: 'Pediatrics' },
  { doctorId: 'DOC-005', name: 'Dr. Ramesh Iyer', specialization: 'Neurology' },
  { doctorId: 'DOC-006', name: 'Dr. Priya Kapoor', specialization: 'Dermatology' },
  { doctorId: 'DOC-007', name: 'Dr. Suresh Joshi', specialization: 'Gastroenterology' },
  { doctorId: 'DOC-008', name: 'Dr. Kavita Menon', specialization: 'Ophthalmology' },
  { doctorId: 'DOC-009', name: 'Dr. Deepak Srinivasan', specialization: 'Pulmonology' },
  { doctorId: 'DOC-010', name: 'Dr. Meera Bhatt', specialization: 'Gynecology' },
  { doctorId: 'DOC-011', name: 'Dr. Venkat Rajan', specialization: 'Nephrology' },
  { doctorId: 'DOC-012', name: 'Dr. Smita Kulkarni', specialization: 'Psychiatry' },
];

const firstNames = [
  'Rajesh', 'Sneha', 'Vikram', 'Lakshmi', 'Anand', 'Kavita', 'Siddharth', 'Rekha',
  'Manoj', 'Geeta', 'Rahul', 'Nandini', 'Arun', 'Shalini', 'Pradeep', 'Deepa',
  'Sanjay', 'Anita', 'Vikas', 'Radhika', 'Amit', 'Sangeeta', 'Nitin', 'Pooja',
  'Harish', 'Vimala', 'Girish', 'Usha', 'Kiran', 'Vani', 'Ravi', 'Lata',
  'Mahesh', 'Neelam', 'Dinesh', 'Shobha', 'Vivek', 'Sarita', 'Ajay', 'Maya',
  'Sunil', 'Divya', 'Ganesh', 'Rohini', 'Pankaj', 'Shweta', 'Akash', 'Bhavna',
  'Sachin', 'Smita', 'Rohit', 'Preeti', 'Vinay', 'Sushma', 'Akshay', 'Tara',
  'Karthik', 'Mala', 'Jayesh', 'Anjali', 'Prasad', 'Varsha', 'Mohan', 'Kamala',
  'Shankar', 'Leela', 'Navneet', 'Alka', 'Omkar', 'Sujata', 'Harshad', 'Nilima',
  'Bharat', 'Jyoti', 'Naveen', 'Kamini', 'Surendra', 'Asha', 'Dinesh', 'Rashmi',
];

const lastNames = [
  'Sharma', 'Patel', 'Verma', 'Singh', 'Kumar', 'Gupta', 'Iyer', 'Nair',
  'Reddy', 'Joshi', 'Das', 'Banerjee', 'Mukherjee', 'Deshmukh', 'Menon',
  'Agarwal', 'Saxena', 'Gokhale', 'Pillai', 'Rao', 'Naidu', 'Khanna',
  'Bhatt', 'Sethi', 'Chopra', 'Kulkarni', 'Desai', 'Pawar', 'Gaikwad',
  'Mhatre', 'Kadam', 'Sawant', 'Patil', 'Thakur', 'Yadav', 'Mishra',
  'Sinha', 'Tiwari', 'Dwivedi', 'Chatterjee', 'Sengupta', 'Bose',
  'Bajaj', 'Sehgal', 'Sood', 'Mehta', 'Choudhury', 'Kohli',
];

const bloodGroups = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];
const cities = [
  'Mumbai', 'Delhi', 'Bangalore', 'Hyderabad', 'Chennai', 'Kolkata',
  'Pune', 'Ahmedabad', 'Jaipur', 'Lucknow', 'Surat', 'Indore',
];

const lowSymptoms = [
  { s: 'Lower back pain radiating to left leg', d: 'Lumbar Disc Herniation', sev: 'LOW' },
  { s: 'Recurrent abdominal pain, bloating, irregular bowels', d: 'Irritable Bowel Syndrome', sev: 'LOW' },
  { s: 'Skin rash with intense itching, redness', d: 'Allergic Contact Dermatitis', sev: 'LOW' },
  { s: 'Dizziness, fatigue, pale skin', d: 'Iron Deficiency Anemia', sev: 'LOW' },
  { s: 'Persistent heartburn, difficulty swallowing', d: 'Gastroesophageal Reflux Disease', sev: 'LOW' },
  { s: 'Ear pain with discharge, reduced hearing', d: 'Chronic Suppurative Otitis Media', sev: 'LOW' },
  { s: 'Burning urination, lower abdominal pain', d: 'Urinary Tract Infection', sev: 'LOW' },
  { s: 'Unilateral facial weakness, inability to close eye', d: "Bell's Palsy", sev: 'LOW' },
  { s: 'Nausea, vomiting, watery diarrhea for 2 days', d: 'Acute Gastroenteritis', sev: 'LOW' },
  { s: 'Swelling in scrotum, dull ache', d: 'Inguinal Hernia', sev: 'LOW' },
  { s: 'Pain in right upper quadrant with fatty food intolerance', d: 'Cholelithiasis', sev: 'LOW' },
  { s: 'Chronic dry cough, occasional wheezing', d: 'Allergic Bronchitis', sev: 'LOW' },
  { s: 'Mild headache, nasal congestion, postnasal drip', d: 'Chronic Sinusitis', sev: 'LOW' },
  { s: 'Tooth pain radiating to jaw, sensitivity to cold', d: 'Dental Caries', sev: 'LOW' },
  { s: 'Mild itching in perianal region, worse at night', d: 'Pinworm Infestation', sev: 'LOW' },
];

const medSymptoms = [
  { s: 'Persistent cough with fever, difficulty breathing', d: 'Community Acquired Pneumonia', sev: 'MEDIUM' },
  { s: 'Severe headache with vomiting, photophobia', d: 'Migraine with Aura', sev: 'MEDIUM' },
  { s: 'Fever, joint pain, rash on extremities', d: 'Dengue Fever', sev: 'MEDIUM' },
  { s: 'Frequent urination, excessive thirst, weight loss', d: 'Type 2 Diabetes Mellitus', sev: 'MEDIUM' },
  { s: 'Sudden onset high fever with rigors', d: 'Malaria', sev: 'MEDIUM' },
  { s: 'Blood in stools, abdominal pain, weight loss', d: 'Ulcerative Colitis', sev: 'MEDIUM' },
  { s: 'Swollen painful knee joint after fall', d: 'Fractured Patella', sev: 'MEDIUM' },
  { s: 'Productive cough with blood-tinged sputum', d: 'Tuberculosis', sev: 'MEDIUM' },
  { s: 'Fever, fatigue, jaundice, dark urine', d: 'Hepatitis A', sev: 'MEDIUM' },
  { s: 'Persistent dry cough, low-grade fever, night sweats', d: 'Pulmonary TB', sev: 'MEDIUM' },
  { s: 'Multiple joint pains with morning stiffness', d: 'Rheumatoid Arthritis', sev: 'MEDIUM' },
  { s: 'Severe anxiety with panic attacks, palpitations', d: 'Generalized Anxiety Disorder', sev: 'MEDIUM' },
  { s: 'Sudden hearing loss in right ear', d: 'Sudden Sensorineural Hearing Loss', sev: 'MEDIUM' },
  { s: 'Abnormal vaginal discharge, pelvic pain', d: 'Pelvic Inflammatory Disease', sev: 'MEDIUM' },
  { s: 'Uncontrolled tremors, rigidity, bradykinesia', d: "Parkinson's Disease", sev: 'MEDIUM' },
  { s: 'Sudden colicky flank pain radiating to groin', d: 'Ureteral Calculus', sev: 'MEDIUM' },
  { s: 'Yellowish discoloration of skin and eyes, itching', d: 'Obstructive Jaundice', sev: 'MEDIUM' },
  { s: 'High fever with rash starting on face spreading downward', d: 'Measles', sev: 'MEDIUM' },
  { s: 'Recurrent episodes of hypoglycemia, palpitations', d: 'Insulinoma (Suspected)', sev: 'MEDIUM' },
  { s: 'Painless progressive vision loss, halos around lights', d: 'Glaucoma', sev: 'MEDIUM' },
  { s: 'Heat intolerance, weight loss, exophthalmos', d: "Graves' Disease / Hyperthyroidism", sev: 'MEDIUM' },
  { s: 'Chronic kidney disease stage 4, fatigue, edema', d: 'Chronic Kidney Disease', sev: 'MEDIUM' },
  { s: 'Fever with chills, headache, muscle pain', d: 'Typhoid Fever', sev: 'MEDIUM' },
  { s: 'Cyanosis, clubbing, recurrent respiratory infections', d: 'Bronchiectasis', sev: 'MEDIUM' },
  { s: 'Recurrent episodes of vertigo with nausea', d: "Meniere's Disease", sev: 'MEDIUM' },
];

const criticalSymptoms = [
  { s: 'Chest pain radiating to left arm, shortness of breath', d: 'Acute Myocardial Infarction', sev: 'CRITICAL' },
  { s: 'Breathlessness on exertion, swelling in ankles', d: 'Congestive Heart Failure', sev: 'CRITICAL' },
  { s: 'Sudden severe abdominal pain with rebound tenderness', d: 'Acute Appendicitis', sev: 'CRITICAL' },
  { s: 'Blurred vision, floaters in right eye', d: 'Retinal Detachment', sev: 'CRITICAL' },
  { s: 'High fever with neck stiffness, altered sensorium', d: 'Meningitis', sev: 'CRITICAL' },
  { s: 'Severe depression, suicidal thoughts', d: 'Major Depressive Disorder', sev: 'CRITICAL' },
  { s: 'Sudden onset right-sided weakness, slurred speech', d: 'Cerebrovascular Accident (Stroke)', sev: 'CRITICAL' },
  { s: 'Vaginal bleeding during pregnancy, abdominal cramps', d: 'Threatened Abortion', sev: 'CRITICAL' },
  { s: 'Breast lump, peau d\'orange appearance', d: 'Infiltrating Ductal Carcinoma', sev: 'CRITICAL' },
  { s: 'Acute onset severe epigastric pain radiating to back', d: 'Acute Pancreatitis', sev: 'CRITICAL' },
  { s: 'Painless rectal bleeding with change in bowel habits', d: 'Colorectal Carcinoma', sev: 'CRITICAL' },
  { s: 'Severe dehydration with oliguria, sunken eyes', d: 'Severe Dehydration (Cholera)', sev: 'CRITICAL' },
  { s: 'Progressive difficulty breathing, wheezing', d: 'Acute Severe Asthma', sev: 'CRITICAL' },
  { s: 'Cough with frothy pink sputum, orthopnea', d: 'Acute Pulmonary Edema', sev: 'CRITICAL' },
  { s: 'Vomiting with hematemesis, melena', d: 'Upper GI Bleed (Gastric Ulcer)', sev: 'CRITICAL' },
];

const medicinesList = [
  { name: 'Amoxicillin 500mg', dosage: '500 mg', frequency: 'Three times daily', duration: '7 days' },
  { name: 'Paracetamol 650mg', dosage: '650 mg', frequency: 'As needed', duration: '5 days' },
  { name: 'Omeprazole 20mg', dosage: '20 mg', frequency: 'Once daily before breakfast', duration: '4 weeks' },
  { name: 'Amlodipine 5mg', dosage: '5 mg', frequency: 'Once daily', duration: '30 days' },
  { name: 'Metformin 500mg', dosage: '500 mg', frequency: 'Twice daily with meals', duration: '90 days' },
  { name: 'Atorvastatin 10mg', dosage: '10 mg', frequency: 'Once daily at bedtime', duration: '90 days' },
  { name: 'Aspirin 75mg', dosage: '75 mg', frequency: 'Once daily', duration: '90 days' },
  { name: 'Losartan 50mg', dosage: '50 mg', frequency: 'Once daily', duration: '30 days' },
  { name: 'Azithromycin 500mg', dosage: '500 mg', frequency: 'Once daily', duration: '3 days' },
  { name: 'Cetirizine 10mg', dosage: '10 mg', frequency: 'Once daily at night', duration: '14 days' },
  { name: 'Salbutamol Inhaler 100mcg', dosage: '100 mcg', frequency: '2 puffs as needed', duration: '30 days' },
  { name: 'Prednisolone 40mg', dosage: '40 mg', frequency: 'Once daily tapering', duration: '14 days' },
  { name: 'Doxycycline 100mg', dosage: '100 mg', frequency: 'Twice daily', duration: '14 days' },
  { name: 'Metronidazole 400mg', dosage: '400 mg', frequency: 'Three times daily', duration: '7 days' },
  { name: 'Clopidogrel 75mg', dosage: '75 mg', frequency: 'Once daily', duration: '90 days' },
  { name: 'Furosemide 40mg', dosage: '40 mg', frequency: 'Once daily in morning', duration: '30 days' },
  { name: 'Pantoprazole 40mg', dosage: '40 mg', frequency: 'Once daily before breakfast', duration: '4 weeks' },
  { name: 'Levetiracetam 500mg', dosage: '500 mg', frequency: 'Twice daily', duration: '90 days' },
  { name: 'Thyroxine 50mcg', dosage: '50 mcg', frequency: 'Once daily on empty stomach', duration: '90 days' },
  { name: 'Warfarin 5mg', dosage: '5 mg', frequency: 'Once daily at same time', duration: '90 days' },
  { name: 'Insulin Regular 100IU/mL', dosage: '10 units', frequency: 'Subcutaneous before meals', duration: '30 days' },
  { name: 'Ciprofloxacin 500mg', dosage: '500 mg', frequency: 'Twice daily', duration: '10 days' },
  { name: 'Iron Sucrose Injection', dosage: '200 mg', frequency: 'IV infusion weekly', duration: '6 weeks' },
  { name: 'Ondansetron 4mg', dosage: '4 mg', frequency: 'As needed for nausea', duration: '3 days' },
  { name: 'Diclofenac Gel 1%', dosage: 'Apply locally', frequency: 'Three times daily', duration: '7 days' },
  { name: 'Acyclovir 400mg', dosage: '400 mg', frequency: 'Five times daily', duration: '7 days' },
  { name: 'Fluconazole 150mg', dosage: '150 mg', frequency: 'Single dose', duration: '1 day' },
  { name: 'Bisoprolol 5mg', dosage: '5 mg', frequency: 'Once daily', duration: '30 days' },
  { name: 'Spironolactone 25mg', dosage: '25 mg', frequency: 'Once daily', duration: '30 days' },
  { name: 'Albuterol Nebulizer Solution', dosage: '2.5 mg', frequency: 'Nebulization every 4-6 hours', duration: '7 days' },
  { name: 'Ranitidine 150mg', dosage: '150 mg', frequency: 'Twice daily', duration: '14 days' },
  { name: 'Cefixime 200mg', dosage: '200 mg', frequency: 'Twice daily', duration: '10 days' },
  { name: 'Tramadol 50mg', dosage: '50 mg', frequency: 'When necessary', duration: '5 days' },
  { name: 'Gabapentin 300mg', dosage: '300 mg', frequency: 'Once daily at bedtime', duration: '30 days' },
  { name: 'Sertraline 50mg', dosage: '50 mg', frequency: 'Once daily in morning', duration: '90 days' },
  { name: 'Lactulose 10mL', dosage: '10 mL', frequency: 'Twice daily', duration: '14 days' },
  { name: 'Multivitamin Supplement', dosage: '1 tablet', frequency: 'Once daily', duration: '30 days' },
  { name: 'Hydrochlorothiazide 12.5mg', dosage: '12.5 mg', frequency: 'Once daily in morning', duration: '30 days' },
  { name: 'Augmentin 625mg', dosage: '625 mg', frequency: 'Twice daily', duration: '10 days' },
  { name: 'Diazepam 5mg', dosage: '5 mg', frequency: 'At bedtime as needed', duration: '7 days' },
];

const labTests = [
  { name: 'Complete Blood Count (CBC)', unit: 'per mcL', normalLow: 4, normalHigh: 11 },
  { name: 'Hemoglobin (Hb)', unit: 'g/dL', normalLow: 12, normalHigh: 16 },
  { name: 'Fasting Blood Sugar (FBS)', unit: 'mg/dL', normalLow: 70, normalHigh: 110 },
  { name: 'Postprandial Blood Sugar (PPBS)', unit: 'mg/dL', normalLow: 80, normalHigh: 140 },
  { name: 'Serum Creatinine', unit: 'mg/dL', normalLow: 0.6, normalHigh: 1.2 },
  { name: 'Blood Urea Nitrogen (BUN)', unit: 'mg/dL', normalLow: 7, normalHigh: 20 },
  { name: 'Serum Sodium', unit: 'mEq/L', normalLow: 135, normalHigh: 145 },
  { name: 'Serum Potassium', unit: 'mEq/L', normalLow: 3.5, normalHigh: 5.1 },
  { name: 'Total Bilirubin', unit: 'mg/dL', normalLow: 0.3, normalHigh: 1.2 },
  { name: 'SGOT (AST)', unit: 'U/L', normalLow: 10, normalHigh: 40 },
  { name: 'SGPT (ALT)', unit: 'U/L', normalLow: 7, normalHigh: 56 },
  { name: 'Alkaline Phosphatase (ALP)', unit: 'U/L', normalLow: 44, normalHigh: 147 },
  { name: 'Total Cholesterol', unit: 'mg/dL', normalLow: 125, normalHigh: 200 },
  { name: 'HDL Cholesterol', unit: 'mg/dL', normalLow: 35, normalHigh: 60 },
  { name: 'LDL Cholesterol', unit: 'mg/dL', normalLow: 0, normalHigh: 130 },
  { name: 'Triglycerides', unit: 'mg/dL', normalLow: 0, normalHigh: 150 },
  { name: 'Uric Acid', unit: 'mg/dL', normalLow: 3.4, normalHigh: 7.0 },
  { name: 'TSH', unit: 'mIU/L', normalLow: 0.4, normalHigh: 4.0 },
  { name: 'Free T4', unit: 'ng/dL', normalLow: 0.8, normalHigh: 1.8 },
  { name: 'HbA1c', unit: '%', normalLow: 4, normalHigh: 5.7 },
  { name: 'Prothrombin Time (PT)', unit: 'sec', normalLow: 11, normalHigh: 13.5 },
  { name: 'INR', unit: '', normalLow: 0.8, normalHigh: 1.2 },
  { name: 'D-Dimer', unit: 'ng/mL', normalLow: 0, normalHigh: 500 },
  { name: 'CRP (Quantitative)', unit: 'mg/L', normalLow: 0, normalHigh: 6 },
  { name: 'ESR', unit: 'mm/hr', normalLow: 0, normalHigh: 20 },
  { name: 'Blood Culture', unit: '', normalLow: 0, normalHigh: 0 },
  { name: 'Urine Routine & Microscopy', unit: '', normalLow: 0, normalHigh: 0 },
  { name: 'ECG (12-Lead)', unit: '', normalLow: 0, normalHigh: 0 },
  { name: 'Chest X-Ray (PA View)', unit: '', normalLow: 0, normalHigh: 0 },
  { name: 'MRI Brain (Plain)', unit: '', normalLow: 0, normalHigh: 0 },
];

const labStatuses = ['ORDERED', 'SAMPLE_COLLECTED', 'PROCESSING', 'RESULT_ENTERED', 'PENDING_APPROVAL', 'APPROVED'];

function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function randomFloat(min, max, decimals = 1) {
  return parseFloat((Math.random() * (max - min) + min).toFixed(decimals));
}

function pick(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

function pickN(arr, n) {
  const shuffled = [...arr].sort(() => 0.5 - Math.random());
  return shuffled.slice(0, n);
}

function generatePhone() {
  const prefixes = ['98765', '99887', '76543', '88990', '77665', '99800', '88776'];
  return `${pick(prefixes)}${String(randomInt(10000, 99999))}`;
}

function generateVitals(severity) {
  const isCritical = severity === 'CRITICAL';
  let bp, hr, temp, spo2, sugar, abnormal = false, reason = '';

  if (isCritical) {
    bp = pick(['190/110', '200/120', '80/50', '70/40', '220/130', '160/110']);
    hr = randomInt(120, 150);
    temp = randomFloat(101.5, 104.0);
    spo2 = randomInt(82, 90);
    sugar = randomFloat(200, 350);
    abnormal = true;
    reason = `Critical vitals: BP ${bp}, HR ${hr}, SpO2 ${spo2}%, Temp ${temp}°F`;
  } else if (Math.random() < 0.2) {
    bp = pick(['150/95', '160/100', '90/60', '145/92']);
    hr = randomInt(95, 110);
    temp = randomFloat(99.5, 101.0);
    spo2 = randomInt(91, 94);
    sugar = randomFloat(140, 180);
    abnormal = true;
    reason = `Abnormal vitals: BP ${bp}, HR ${hr}, SpO2 ${spo2}%`;
  } else {
    bp = pick(['120/80', '118/76', '122/82', '116/78', '124/84', '110/70', '126/80', '130/85']);
    hr = randomInt(72, 88);
    temp = randomFloat(97.6, 99.0);
    spo2 = randomInt(96, 100);
    sugar = randomFloat(85, 120);
  }

  return { bp, heartRate: hr, temperature: temp, spo2, sugarLevel: sugar, abnormalFlag: abnormal, alertReason: abnormal ? reason : null };
}

function generatePrescriptions(count) {
  const meds = pickN(medicinesList, count);
  return meds.map((m) => ({
    ...m,
    adherenceStatus: Math.random() < 0.15 ? 'MISSED' : (Math.random() < 0.2 ? 'PARTIAL' : 'COMPLETE'),
  }));
}

function generateLabResult(test, isAbnormal) {
  if (test.name.includes('X-Ray') || test.name.includes('ECG') || test.name.includes('MRI') || test.name.includes('Culture') || test.name.includes('Urine')) {
    const normalFindings = [
      'Normal study, no significant abnormality detected',
      'Normal sinus rhythm, no ST changes',
      'No growth after 48 hours',
      'Within normal limits',
    ];
    const abnormalFindings = [
      'Bilateral infiltrates suggestive of pneumonia',
      'Pus cells 10-12/hpf, epithelial cells present',
      'Left ventricular hypertrophy noted',
      'Acute infarct in right middle cerebral artery territory',
    ];
    return isAbnormal ? pick(abnormalFindings) : pick(normalFindings);
  }

  const value = isAbnormal
    ? randomFloat(test.normalHigh * 1.5, test.normalHigh * 3.0, 1)
    : randomFloat(test.normalLow + (test.normalHigh - test.normalLow) * 0.3, test.normalHigh * 0.85, 1);

  return `${value} ${test.unit || ''}`;
}

const duplicatePairs = [
  { firstIdx: 0, secondIdx: 95, firstName: 'Rajesh', lastName: 'Sharma' },
  { firstIdx: 1, secondIdx: 96, firstName: 'Sneha', lastName: 'Patel' },
  { firstIdx: 2, secondIdx: 97, firstName: 'Vikram', lastName: 'Singh' },
  { firstIdx: 3, secondIdx: 98, firstName: 'Lakshmi', lastName: 'Verma' },
  { firstIdx: 4, secondIdx: 99, firstName: 'Anand', lastName: 'Kumar' },
];
function getDuplicateName(index) {
  const pair = duplicatePairs.find(p => p.firstIdx === index || p.secondIdx === index);
  if (!pair) return null;
  return { firstName: pair.firstName, lastName: pair.lastName };
}

function generateRecord(index) {
  const isCritical = index < 10;
  const severity = isCritical ? 'CRITICAL' : 'NON_CRITICAL';

  const isFollowUp = index >= 80 && index < 90;

  const namePair = getDuplicateName(index);
  const firstName = namePair ? namePair.firstName : pick(firstNames);
  const lastName = namePair ? namePair.lastName : pick(lastNames);

  const gender = pick(['Male', 'Female']);
  const age = isCritical ? randomInt(45, 80) : randomInt(18, 75);
  const bloodGroup = pick(bloodGroups);
  const city = pick(cities);
  const address = `${randomInt(1, 999)}, ${pick(['MG Road', 'Park Street', 'Main Road', 'Lake View', 'Sector ' + randomInt(1, 20), 'Gandhi Nagar', 'Indira Nagar', 'Civil Lines', 'Hiranandani Estate', 'Koramangala'])}`;

  const patientId = uuid();
  const phone = generatePhone();

  const doctor = pick(doctors);
  const minDays = isCritical ? -5 : (isFollowUp ? -45 : -60);
  const maxDays = isCritical ? 3 : (isFollowUp ? -1 : 30);
  const appointmentDate = new Date();
  appointmentDate.setDate(appointmentDate.getDate() + randomInt(minDays, maxDays));

  let appStatus;
  if (isCritical) {
    const now = new Date();
    appStatus = appointmentDate > now ? 'SCHEDULED' : pick(['CHECKED_IN', 'IN_CONSULTATION', 'COMPLETED']);
  } else {
    appStatus = pick(['SCHEDULED', 'CHECKED_IN', 'IN_CONSULTATION', 'COMPLETED', 'NO_SHOW']);
  }

  // Pick symptom based on severity
  let symptom;
  if (isCritical) {
    symptom = pick(criticalSymptoms);
  } else {
    symptom = Math.random() < 0.4 ? pick(lowSymptoms) : pick(medSymptoms);
  }

  const vitals = generateVitals(severity);

  const prescriptionCount = randomInt(2, 5);
  const prescriptions = generatePrescriptions(prescriptionCount);

  const hasAbnormalLab = index >= 10 && index < 20; // indices 10-19 = 10% abnormal
  const labCount = isCritical ? randomInt(3, 5) : (hasAbnormalLab ? randomInt(1, 3) : randomInt(0, 3));
  const labs = [];
  const labTestsToUse = pickN(labTests, labCount);
  for (let i = 0; i < labTestsToUse.length; i++) {
    const lt = labTestsToUse[i];
    const labStatus = pick(labStatuses);
    const isAbnormal = hasAbnormalLab;
    labs.push({
      testId: `LAB-${String(index + 1).padStart(3, '0')}-${String(i + 1).padStart(2, '0')}`,
      testName: lt.name,
      status: labStatus,
      resultValue: generateLabResult(lt, isAbnormal),
      normalRange: lt.unit ? `${lt.normalLow} - ${lt.normalHigh} ${lt.unit}` : 'See report',
      criticalFlag: isAbnormal,
      orderedBy: doctor.name,
      orderedDate: new Date(appointmentDate.getTime() - randomInt(0, 3) * 3600000).toISOString(),
    });
  }

  const pharmacyItems = [];
  for (const p of prescriptions) {
    const batchNum = `BATCH-${String(randomInt(1000, 9999))}-${String(randomInt(10, 99))}`;
    const expiry = new Date();
    expiry.setFullYear(expiry.getFullYear() + randomInt(1, 2));
    const qty = randomInt(10, 60);
    const dispensed = Math.random() < 0.15 ? Math.floor(qty * 0.5) : qty;
    const partial = dispensed < qty;
    pharmacyItems.push({
      medicineName: p.name,
      batchNumber: batchNum,
      expiryDate: expiry.toISOString().split('T')[0],
      prescribedQuantity: qty,
      dispensedQuantity: dispensed,
      dispenseStatus: partial ? 'PARTIAL' : 'FULL',
    });
  }

  const billingAmount = randomFloat(500, 50000, 2);
  const hasPendingPayment = index >= 20 && index < 35; // indices 20-34 = 15% pending
  const hasRefund = index >= 35 && index < 38; // indices 35-37 = 3% refund
  let paidAmount, billingStatus;
  if (hasRefund) {
    paidAmount = billingAmount;
    billingStatus = 'PAID';
  } else if (hasPendingPayment) {
    paidAmount = randomFloat(100, billingAmount * 0.7, 2);
    billingStatus = 'PARTIAL';
  } else {
    paidAmount = billingAmount;
    billingStatus = 'PAID';
  }

  const isDuplicate = duplicatePairs.some(p => p.firstIdx === index || p.secondIdx === index);

  return {
    patient: {
      patientId,
      firstName,
      lastName,
      fullName: `${firstName} ${lastName}`,
      age,
      gender,
      dateOfBirth: new Date(new Date().setFullYear(new Date().getFullYear() - age)).toISOString().split('T')[0],
      phone,
      bloodGroup,
      address: `${address}, ${city}`,
      city,
    },
    appointment: {
      appointmentId: `APT-${String(index + 1).padStart(3, '0')}`,
      patientId,
      doctorId: doctor.doctorId,
      doctorName: doctor.name,
      specialization: doctor.specialization,
      appointmentDate: appointmentDate.toISOString().split('T')[0],
      startTime: `${String(randomInt(8, 17)).padStart(2, '0')}:${String(randomInt(0, 3) * 15).padStart(2, '0')}`,
      status: appStatus,
      severity,
    },
    clinical: {
      symptoms: symptom.s,
      diagnosis: symptom.d,
      severity,
      isEmergency: isCritical,
    },
    vitals,
    prescription: {
      items: prescriptions,
      totalMedicines: prescriptions.length,
      missedDoses: prescriptions.filter(p => p.adherenceStatus === 'MISSED').length,
    },
    lab: {
      tests: labs,
      hasPendingResults: labs.some(l => l.status !== 'APPROVED'),
      hasCriticalResults: labs.some(l => l.criticalFlag),
    },
    pharmacy: {
      items: pharmacyItems,
      totalCharges: pharmacyItems.reduce((sum, i) => sum + i.dispensedQuantity * randomFloat(5, 50, 2), 0),
    },
    billing: {
      billId: `BILL-${String(index + 1).padStart(3, '0')}`,
      patientId,
      totalAmount: billingAmount,
      paidAmount,
      pendingAmount: Math.max(0, parseFloat((billingAmount - paidAmount).toFixed(2))),
      status: billingStatus,
      refunded: hasRefund,
      refundAmount: hasRefund ? randomFloat(50, billingAmount * 0.2, 2) : 0,
      refundReason: hasRefund ? pick(['Patient overpaid', 'Insurance adjustment', 'Service not rendered', 'Duplicate billing correction']) : null,
      paymentMode: billingStatus === 'PAID' ? pick(['CASH', 'CARD', 'UPI', 'INSURANCE', 'NET_BANKING']) : 'PENDING',
    },
    metadata: {
      isFollowUp,
      previousVisitDays: isFollowUp ? randomInt(7, 45) : null,
      hasMissedMedication: prescriptions.some(p => p.adherenceStatus === 'MISSED'),
      isCriticalPatient: isCritical,
      isDuplicateName: isDuplicate,
    },
  };
}

const records = [];
for (let i = 0; i < 100; i++) {
  records.push(generateRecord(i));
}

const output = JSON.stringify(records, null, 2);
const outputPath = 'scripts/hospital-data-100-records.json';
fs.writeFileSync(outputPath, output, 'utf-8');

// Stats
const criticalCount = records.filter(r => r.metadata.isCriticalPatient).length;
const abnormalLabCount = records.filter(r => r.lab.hasCriticalResults).length;
const pendingPaymentCount = records.filter(r => r.billing.status !== 'PAID').length;
const duplicateNames = records.filter(r => r.metadata.isDuplicateName).length;
const followUpCount = records.filter(r => r.metadata.isFollowUp).length;
const missedMedCount = records.filter(r => r.metadata.hasMissedMedication).length;
const refundCount = records.filter(r => r.billing.refunded).length;
const noShowCount = records.filter(r => r.appointment.status === 'NO_SHOW').length;

console.log('=== GENERATION SUMMARY ===');
console.log(`Total records: ${records.length}`);
console.log(`Critical patients: ${criticalCount} (${((criticalCount/100)*100).toFixed(0)}%)`);
console.log(`Abnormal lab results: ${abnormalLabCount} (${((abnormalLabCount/100)*100).toFixed(0)}%)`);
console.log(`Pending/Partial payments: ${pendingPaymentCount} (${((pendingPaymentCount/100)*100).toFixed(0)}%)`);
console.log(`Duplicate names: ${duplicateNames} records (${duplicateNames/2} pairs = 5%)`);
console.log(`Follow-up visits: ${followUpCount} (${((followUpCount/100)*100).toFixed(0)}%)`);
console.log(`Missed medications: ${missedMedCount} (${((missedMedCount/100)*100).toFixed(0)}%)`);
console.log(`Refunds processed: ${refundCount} (${((refundCount/100)*100).toFixed(0)}%)`);
console.log(`No-show appointments: ${noShowCount} (${((noShowCount/100)*100).toFixed(0)}%)`);
console.log(`\nOutput written to: ${outputPath}`);

// Verify duplicate names
console.log('\n=== DUPLICATE NAME CHECK ===');
const nameGroups = {};
records.forEach((r, i) => {
  const key = `${r.patient.firstName} ${r.patient.lastName}`;
  if (!nameGroups[key]) nameGroups[key] = [];
  nameGroups[key].push(i);
});
Object.entries(nameGroups).filter(([, v]) => v.length > 1).forEach(([name, indices]) => {
  console.log(`  "${name}" appears at indices: ${indices.join(', ')}`);
});
