# How to start

```shell
cd "$DATA_API_DIR"
python3 -m pip install --user invoke
python3 -m invoke --list
```

## Build in Docker

This project needs Docker buildx for building x86_64 images on Mac M1.

```
  $ brew install colima docker docker-buildx
  $ colima start
  $ docker buildx ls
```
