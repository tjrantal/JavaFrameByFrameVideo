import os, os.path
import shutil
import string
jmfLib='lib\\'
compiledClasses='build\\java\\'
whereTo='libMinimal\\'
for root, dirs, files in os.walk(jmfLib):
	for f in files:
		testFile = compiledClasses+root[len(jmfLib):len(root)]+"\\"+f
		#print([testFile])
		if not os.path.exists(testFile):
			print(testFile)
			copyTargetPath = whereTo+root[len(jmfLib):len(root)]
			if not os.path.exists(copyTargetPath):
				os.makedirs(copyTargetPath)
			shutil.copy(root+"\\"+f,copyTargetPath+"\\"+f)
			
