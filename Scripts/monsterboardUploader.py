import pandas as pd
from os import listdir, rename, path, remove
from shutil import copyfile
from os.path import abspath, basename, splitext
import time
import shlex
from subprocess import Popen, PIPE
import smtplib
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
from email.mime.image import MIMEImage

respondsCsv = "Aanmeld formulier elevator pitch.csv"
filesPath = "..\Videos\RawRecordings\/"

html_filename = "mail.html"
fromaddr = "hello@frame.camera"

def sendMail(adres, name):
	msg = MIMEMultipart()
	msg['From'] = "no-reply@monsterboard.nl"
	msg['To'] = adres
	msg['Subject'] = "Je Monsterpitch staat online!"
	body = open(html_filename).read()
	body = body.replace('[naam invoegen]', name)

	imagePath = '../Afbeeldingen/MailImages/'
	for file in listdir(imagePath):
		fp = open(imagePath + file, 'rb')
		msgImage = MIMEImage(fp.read())
		fp.close()
		msgImage.add_header('Content-ID', '<image' + basename(file) + '>')
		msg.attach(msgImage)

	msg.attach(MIMEText(body, 'html'))
	server = smtplib.SMTP('mail.frame.camera', 587)
	server.login(fromaddr, "")
	server.sendmail(fromaddr, adres, msg.as_string())
	server.quit()

def representsInt(string):
    try: 
        int(string)
        return True
    except ValueError:
        return False

def moveFileToFolder(filePath, des):
	print (filePath + " :: " + des + basename(filePath))
	try:
		rename(filePath, des + basename(filePath))
	except OSError as e:
		print("File already existed?")

def copyFileToFolder(filePath, des):
	copyfile(filePath, des + basename(filePath))

def addOverlay(filePath):
	newLocation = "../Videos/EditedRecordings/"
	ffmpegCmd = "ffmpeg -loglevel quiet -y -i \"{0}\" -i ../Afbeeldingen/monsterboardWatermark1080.png -filter_complex \"[0:v][1:v] overlay=x=(main_w-15-overlay_w):y=(main_h-overlay_h)\" -pix_fmt yuv420p -c:a copy {1}".format(filePath, newLocation + basename(filePath))
	process = Popen(shlex.split(ffmpegCmd))
	if process.wait() == 0:
		print ("Added overlay to file: " + filePath)
		remove(filePath)
		return newLocation + basename(filePath)
	else:
		print ("Error overlaying file: " + filePath)
		input("Press enter to continue")
		return

def uploadVideo(filePath, name):
	cmd = "python upload_video.py --file=\"{0}\" --title=\"Monsterpitch {1}\" --description=\"{1}\"".format(filePath, name)
	process = Popen(shlex.split(cmd), stdout=PIPE)
	process.communicate()
	if process.wait() == 0:
		moveFileToFolder(filePath, "..\/Videos\/UploadedRecordings\/");
		print ("Uploaded file {0} with title {1}".format(filePath, name))
		return True
	else:
		print ("Error uploading: " + cmd)
		return False

if __name__ == '__main__':
	responds = pd.read_csv(respondsCsv)
	responds["filePath"] = ""

	# Add filepath of recordings in raw folder to responds dataframe
	for file in listdir(filesPath):
		file = file.lower()
		if file.endswith('.mp4'):
			fileNumber = file.replace('.mp4', '')
			if representsInt(fileNumber):
				responds.loc[responds['Wachtnummer'] == int(fileNumber), 'filePath'] = abspath(filesPath + file)

 	# For each response that has a filepath assigned
	for row in responds.iterrows():
		value = row[1]
		filePath = value['filePath']
		if(filePath != ""):
			print ("Starting process for: {} {} {} {}".format(filePath, value['Wachtnummer'], value['Voornaam'], value['Achternaam']))
			print ("Copying file to backup")
			copyFileToFolder(filePath, "../\Videos/\Backup\/")
			print ("Adding overlay")
			overlayedFile = addOverlay(filePath)
			if(overlayedFile != ""):
				if(uploadVideo(overlayedFile, value['Voornaam'] + " " + value['Achternaam'])):
					sendMail(value['Gebruikersnaam'], value['Voornaam'])

	input("Press enter to quit")