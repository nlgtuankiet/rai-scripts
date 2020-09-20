const exec = require('child_process').exec;
const util = require('util');
const axios = require('axios');
function getSeconds() {
  let d = new Date()
  return d.getSeconds()
}
async function execute(command) {
  const job = new Promise(function (resolve, reject) {
    exec(command, function (error, stdout, stderr) {
      if (stderr || error) {
        if (stderr) {
          reject(stderr)
        } else {
          reject(error)
        }
      } else {
        resolve(stdout)
      }
    });
  })
  return await job
}

let token = null
let lastRefreshTokenTime = getSeconds()

async function ensureToken() {
  async function refreshToken() {
    console.log("refreshing token")
    token = await execute("gcloud auth print-access-token rai-520@tikiandroid-1047.iam.gserviceaccount.com")
    token = token.trim()
    lastRefreshTokenTime = getSeconds()
  }

  if (!token) {
    await refreshToken()
  }
  const currentTime = getSeconds()
  if (currentTime - lastRefreshTokenTime > 30 * 60) {
    await refreshToken()
  }
}



async function getItemIds() {
  await ensureToken()
  const response = await axios({
    method: 'get',
    url: "https://recommendationengine.googleapis.com/v1beta1/projects/tikiandroid-1047/locations/global/catalogs/default_catalog/catalogItems/",
    headers: {
      "Authorization": `Bearer ${token}`
    }
  })
  return response.data.catalogItems.map((e) => e.id)
}

async function deleteItem(id) {
  console.log(`delete ${id}`)
  await ensureToken()
  const response = await axios({
    method: 'delete',
    url: `https://recommendationengine.googleapis.com/v1beta1/projects/tikiandroid-1047/locations/global/catalogs/default_catalog/catalogItems/${id.trim()}`,
    headers: {
      "Authorization": `Bearer ${token}`
    }
  })
  if (response.status !== 200) {
    throw Error(`Failed to delete ${id} status: ${response.status} data: ${response.data}`)
  }
}

async function main() {
  let itemIds = await getItemIds()
  let deleteCount = 0
  let startTime = getSeconds()
  while (itemIds.length !== 0) {
    startTime = getSeconds()
    let tasks = itemIds.map((e) => deleteItem(e))
    await Promise.all(tasks)
    let endTime = getSeconds()
    console.log(`Deleted ${deleteCount} items, speed: ${(itemIds.length / (endTime - startTime))} items/s`)
    itemIds = await getItemIds()
  }
}

main()