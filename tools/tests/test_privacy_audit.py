"""Security-boundary regression tests; fixtures contain no real personal data."""
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from privacy_audit import audit_git, findings


class PublicationPrivacyTest(unittest.TestCase):
    def test_private_paths_and_outputs_are_rejected_across_platforms(self):
        for name in ['.env.production', '.dev.vars', 'nested/KEYSTORE.JKS', 'app/build/report.json',
                     'web/node_modules/package/index.js', '.local-archive/source.json',
                     'phone/personal_library.xml', 'backup.sqlite', 'deliverables/app.apk',
                     r'folder\service-account-owner.json', 'key.properties']:
            with self.subTest(name=name):
                self.assertTrue(findings(name, b''))

    def test_publishable_source_and_examples_remain_allowed(self):
        for name in ['.env.example', 'app/src/main/res/xml/backup_rules.xml',
                     'gradle/wrapper/gradle-wrapper.jar', 'web/package-lock.json', 'docs/INSTALL.md']:
            with self.subTest(name=name):
                self.assertEqual([], findings(name, b'non-secret example'))

    def test_profile_identifiers_are_detected_without_reporting_values(self):
        for prefix in ['C:' + chr(92) + 'Users' + chr(92), '/Users/' , '/home/']:
            value = prefix + 'SYNTHETIC_PERSON/sdk'
            self.assertIn('personal profile path', findings('guide.md', value.encode()))
        self.assertEqual([], findings('guide.md', b'C:/Users/YOUR_USERNAME/sdk'))

    def test_private_key_and_usb_identifier_detection(self):
        marker = ('-----BEGIN ' + 'PRIVATE KEY-----').encode()
        self.assertIn('credential pattern', findings('config.txt', marker))
        self.assertIn('hardcoded USB identifier', findings('test.py', b'serial = "' + b'1a2b3c4d' + b'"'))

    def test_staged_scan_cannot_be_fooled_by_a_clean_unstaged_file(self):
        with tempfile.TemporaryDirectory() as folder:
            subprocess.run(['git','init','-q',folder],check=True)
            path=Path(folder)/'config.txt'
            path.write_text('-----BEGIN ' + 'PRIVATE KEY-----',encoding='utf-8')
            subprocess.run(['git','-C',folder,'add','config.txt'],check=True)
            path.write_text('clean working copy',encoding='utf-8')
            self.assertEqual([],audit_git(folder)[1])
            self.assertEqual('credential pattern',audit_git(folder,staged=True)[1][0]['reason'])


if __name__ == '__main__':
    unittest.main()
