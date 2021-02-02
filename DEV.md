# Developer notes

## RegionMap
To update the region map execute the following lines in your terminal:

```
for region in $(aws ec2 describe-regions --query "Regions[].RegionName" --output text); do ami=$(aws --region $region ec2 describe-images --filters "Name=name,Values=amzn2-ami-hvm-2.0.20200917.0-x86_64-gp2" --query "Images[0].ImageId" --output "text"); printf "'$region':\n  AMI: '$ami'\n"; done
```

## Manually Update ClamAV db

```
freshclam
```

## Fetch infected examples

> Install and configure clamd first (e.g. use the template.yaml)

```
curl 'https://objective-see.com/malware.json' | jq -r '.malware[].download' | grep '.zip' | while read -r line; do 
  wget "$line" -O temp.zip;
  unzip -P infect3d temp.zip;
  rm temp.zip
done
chmod -R a+r *
rm -rf FakeFileOpener

find . -name readme.txt | while read -r line; do
  cat "$line" | sed -e '1,/file(s):/d' | grep . | sed "s#^#${line::-10}#" | awk -F ' -> ' '{print $1}'; 
done | while read -r line; do
  if [ -f "$line" ]; then
    echo "$line"; 
  fi
done | while read -r line; do
  clamdscan "${line:2}"
done | grep 'FOUND' | awk -F ':' '{print $1}' | while read -r line; do
  aws s3 cp --acl bucket-owner-full-control "$line" "s3://widdix-aws-s3-virusscan-infected-examples/${line:5}"
done
```

## Simulate load

```
mkdir loadtest/
cd loadtest/
for i in {1..10000}; do touch "file${i}.txt"; done
aws s3 sync . s3://bucketname
```

## Test

### Extensive test

Thanks to [Objective-See](https://objective-see.com/) for providing infected files that we use for testing. Download one of the files upload it to your S3 bucket for testing.
We also have automated tests in place!

### Simple test

Create a [EICAR Standard Anti-Virus Test File](https://en.wikipedia.org/wiki/EICAR_test_file) with the following content:

```
X5O!P%@AP[4\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*
```

and upload that file to your S3 bucket.
