FROM node:alpine

WORKDIR /usr/app

COPY ./package.json ./
COPY ./package-lock.json ./
RUN npm ci
COPY ./*.js ./
COPY ./.env ./

CMD ["npm", "start"]